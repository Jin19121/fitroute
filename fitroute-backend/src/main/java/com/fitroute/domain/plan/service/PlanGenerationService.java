// src/main/java/com/fitroute/domain/plan/service/PlanGenerationService.java
package com.fitroute.domain.plan.service;

import com.fitroute.domain.food.entity.Food;
import com.fitroute.domain.food.repository.FoodRepository;
import com.fitroute.domain.log.service.LogService;
import com.fitroute.domain.plan.entity.DailyPlan;
import com.fitroute.domain.plan.entity.PlanItem;
import com.fitroute.domain.plan.repository.DailyPlanRepository;
import com.fitroute.domain.plan.repository.PlanItemRepository;
import com.fitroute.domain.user.entity.UserProfile;
import com.fitroute.domain.user.event.UserSignedUpEvent;
import com.fitroute.domain.user.repository.UserProfileRepository;
import com.fitroute.global.ai.GeminiClient;
import com.fitroute.global.ai.GeminiPromptBuilder;
import com.fitroute.global.ai.GeminiResponseParser;
import com.fitroute.global.ai.GeminiResponseParser.SelectedMealDto;
import com.fitroute.global.enums.PlanItemCategory;
import com.fitroute.global.enums.PlanItemStatus;
import com.fitroute.global.enums.PlanItemType;
import com.fitroute.global.util.CalorieCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanGenerationService {

    private final DailyPlanRepository dailyPlanRepository;
    private final PlanItemRepository planItemRepository;
    private final UserProfileRepository userProfileRepository;

    private final FoodRepository foodRepository;

    private final GeminiClient geminiClient;
    private final GeminiPromptBuilder promptBuilder;
    private final GeminiResponseParser responseParser;

    private final LogService logService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUserSignedUp(
            UserSignedUpEvent event) {

        doGenerate(
                event.userId(),
                LocalDate.now());
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateForUser(Long userId) {

        doGenerate(
                userId,
                LocalDate.now());
    }

    // ─────────────────────────────────────────────
    // 핵심 생성 로직
    // ─────────────────────────────────────────────

    private void doGenerate(
            Long userId,
            LocalDate date) {

        log.info(
                "[PlanGeneration] Start - userId={}, date={}",
                userId,
                date);

        // 1. 기존 플랜 조회 → version 계산
        List<DailyPlan> existingPlans = dailyPlanRepository
                .findByUserIdAndPlanDateOrderByVersionDesc(
                        userId,
                        date);

        int newVersion = 1;
        Long rootPlanId = null;

        if (!existingPlans.isEmpty()) {

            DailyPlan latest = existingPlans.get(0);

            newVersion = latest.getVersion() + 1;

            rootPlanId = latest.getRootPlanId();

            // ACTIVE → SUPERSEDED
            existingPlans.stream()
                    .filter(p -> p.getStatus() == DailyPlan.PlanStatus.ACTIVE)
                    .forEach(DailyPlan::supersede);

            log.info(
                    "[PlanGeneration] Superseding v{} → creating v{}",
                    latest.getVersion(),
                    newVersion);
        }

        // 2. GENERATING Plan 생성
        DailyPlan dailyPlan = DailyPlan.builder()
                .userId(userId)
                .planDate(date)
                .version(newVersion)
                .rootPlanId(rootPlanId)
                .build();

        dailyPlanRepository.save(dailyPlan);

        try {

            UserProfile profile = userProfileRepository
                    .findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException(
                            "UserProfile not found for userId="
                                    + userId));

            int calorieTarget = calculateCalorieTarget(profile);

            // ─────────────────────────────────────
            // 식단 생성 (DB 기반)
            // ─────────────────────────────────────

            List<PlanItem> mealItems = generateMealItems(
                    profile,
                    dailyPlan,
                    date,
                    calorieTarget);

            // ─────────────────────────────────────
            // 운동 생성 (AI 자유 생성)
            // ─────────────────────────────────────

            String workoutSystemInstruction = promptBuilder.getPlanSystemInstruction();

            String workoutPrompt = promptBuilder.buildWorkoutPrompt(profile);

            String workoutRaw = geminiClient.call(
                    workoutSystemInstruction,
                    workoutPrompt);

            String workoutJson = responseParser.extractText(
                    workoutRaw);

            List<PlanItem> workoutItems = responseParser.parsePlanItems(
                    workoutJson,
                    dailyPlan);

            // ─────────────────────────────────────
            // 통합 저장
            // ─────────────────────────────────────

            List<PlanItem> allItems = new ArrayList<>();

            allItems.addAll(mealItems);
            allItems.addAll(workoutItems);

            planItemRepository.saveAll(allItems);

            Map<String, Object> mealPlan = responseParser.parseMealPlan(
                    workoutJson);

            Map<String, Object> workoutPlan = responseParser.parseWorkoutPlan(
                    workoutJson);

            Map<String, Object> aiMeta = Map.of(
                    "generatedAt",
                            LocalDateTime.now().toString(),

                    "version",
                            newVersion,

                    "mealSource",
                            "DB",

                    "workoutSource",
                    "AI");

            dailyPlan.complete(
                    calorieTarget,
                    mealPlan,
                    workoutPlan,
                    aiMeta);

            // Log 초기화
            if (newVersion > 1) {

                logService.resetForNewPlan(
                        dailyPlan);

            } else {

                logService.createForPlan(
                        dailyPlan);
            }

            log.info(
                    "[PlanGeneration] Success v{} - userId={}, meal={}개, workout={}개",
                    newVersion,
                    userId,
                    mealItems.size(),
                    workoutItems.size());

        } catch (Exception e) {

            dailyPlan.fail();

            log.error(
                    "[PlanGeneration] Failed - userId={}, error={}",
                    userId,
                    extractMeaningfulMessage(e),
                    e);
        }
    }

    // ─────────────────────────────────────────────
    // 식단 생성 (DB 기반)
    // ─────────────────────────────────────────────

    private List<PlanItem> generateMealItems(
            UserProfile profile,
            DailyPlan dailyPlan,
            LocalDate date,
            int calorieTarget) {

        // 1. 사용자용 메뉴 구성
        List<Food> availableFoods = buildMenuForUser(profile);

        if (availableFoods.isEmpty()) {

            log.warn(
                    "[PlanGeneration] foods 테이블이 비어있음. 식단 생략.");

            return List.of();
        }

        // 2. AI에게 ID 선택 요청
        String prompt = promptBuilder.buildMealSelectionPrompt(
                profile,
                availableFoods,
                calorieTarget);

        String rawResponse = geminiClient.call(
                getMealSystemInstruction(),
                prompt);

        String planJson = responseParser.extractText(
                rawResponse);

        List<SelectedMealDto> selected = responseParser.parseMealSelection(
                planJson);

        // 3. 실패 시 fallback
        if (selected.isEmpty()) {

            log.warn(
                    "[PlanGeneration] AI 식단 선택 결과 없음. 기본 식단 사용.");

            return buildFallbackMealItems(
                    availableFoods,
                    dailyPlan,
                    date);
        }

        // 4. DB 확정값 조회
        Map<Long, Food> foodMap = foodRepository
                .findByIdIn(
                        selected.stream()
                                .map(SelectedMealDto::foodId)
                                .toList())
                .stream()
                .collect(Collectors.toMap(
                        Food::getId,
                        f -> f));

        // 5. PlanItem 생성
        List<PlanItem> items = new ArrayList<>();

        for (SelectedMealDto s : selected) {

            Food food = foodMap.get(s.foodId());

            if (food == null) {

                log.warn(
                        "[PlanGeneration] AI가 선택한 foodId={} DB에 없음. 스킵.",
                        s.foodId());

                continue;
            }

            PlanItemCategory category = switch (s.mealType()) {

                case "BREAKFAST" ->
                    PlanItemCategory.BREAKFAST;

                case "LUNCH" ->
                    PlanItemCategory.LUNCH;

                case "DINNER" ->
                    PlanItemCategory.DINNER;

                default ->
                    PlanItemCategory.SNACK;
            };

            items.add(
                    PlanItem.builder()
                            .dailyPlan(dailyPlan)
                            .date(date)
                            .type(PlanItemType.MEAL)
                            .category(category)

                            .foodName(food.getName())

                            // DB 확정값 사용
                            .calories(food.getCalories())
                            .protein(food.getProtein())
                            .fat(food.getFat())
                            .carbs(food.getCarbs())

                            .status(PlanItemStatus.PENDING)
                            .build());
        }

        log.info(
                "[PlanGeneration] 식단 생성 완료 - {}개 (DB 기반)",
                items.size());

        return items;
    }

    // ─────────────────────────────────────────────
    // 식단 스타일별 메뉴 구성
    // ─────────────────────────────────────────────

    private List<Food> buildMenuForUser(
            UserProfile profile) {

        String tag = switch (profile.getDietStyle()) {

            case LOW_CALORIE ->
                "저칼로리";

            case LOW_CARB_HIGH_PROTEIN ->
                "고단백";

            default ->
                "";
        };

        // BALANCED → 전체 반환
        if (tag.isBlank()) {
            return foodRepository.findAll();
        }

        // 태그 음식
        List<Food> tagged = foodRepository.findAllByTagsContaining(tag);

        // 일반 음식 일부 섞기
        List<Food> general = foodRepository.findAll().stream()
                .filter(f -> !tagged.contains(f))
                .limit(20)
                .toList();

        List<Food> combined = new ArrayList<>(tagged);

        combined.addAll(general);

        return combined;
    }

    // ─────────────────────────────────────────────
    // 식단 선택용 AI System Instruction
    // ─────────────────────────────────────────────

    private String getMealSystemInstruction() {

        return """
                Role: Nutritionist AI for FitRoute.
                Output: JSON ONLY. No markdown, no explanation.
                Task: Select food IDs from the provided list to compose today's meals.
                Schema: {"meals":[{"id":int,"meal_type":"BREAKFAST|LUNCH|DINNER"}]}
                """;
    }

    // ─────────────────────────────────────────────
    // Fallback Meal 생성
    // ─────────────────────────────────────────────

    private List<PlanItem> buildFallbackMealItems(
            List<Food> foods,
            DailyPlan dailyPlan,
            LocalDate date) {

        List<PlanItem> items = new ArrayList<>();

        List<PlanItemCategory> mealCategories = List.of(
                PlanItemCategory.BREAKFAST,
                PlanItemCategory.LUNCH,
                PlanItemCategory.DINNER);

        for (PlanItemCategory cat : mealCategories) {

            foods.stream()
                    .filter(f -> f.getCategory() == cat)
                    .findFirst()
                    .ifPresent(food -> items.add(
                            PlanItem.builder()
                                    .dailyPlan(dailyPlan)
                                    .date(date)
                                    .type(PlanItemType.MEAL)
                                    .category(cat)

                                    .foodName(food.getName())

                                    .calories(food.getCalories())
                                    .protein(food.getProtein())
                                    .fat(food.getFat())
                                    .carbs(food.getCarbs())

                                    .status(PlanItemStatus.PENDING)
                                    .build()));
        }

        return items;
    }

    // ─────────────────────────────────────────────
    // 목표 칼로리 계산
    // ─────────────────────────────────────────────

    private int calculateCalorieTarget(
            UserProfile profile) {

        double bmr = CalorieCalculator.calculateBMR(profile);

        double tdee = CalorieCalculator.calculateTDEE(
                bmr,
                profile.getActivityLevel());

        return CalorieCalculator.calculateTargetCalories(
                tdee,
                profile.getWeight(),
                profile.getTargetWeight(),
                profile.getTargetPeriod(),
                profile.getGender());
    }

    // ─────────────────────────────────────────────
    // Error Message 정리
    // ─────────────────────────────────────────────

    private String extractMeaningfulMessage(
            Exception e) {

        if (e instanceof HttpClientErrorException httpEx) {

            String body = httpEx.getResponseBodyAsString();

            return "HTTP "
                    + httpEx.getStatusCode()
                    + ": "
                    + (body.length() > 500
                            ? body.substring(0, 500)
                                    + "...[truncated]"
                            : body);
        }

        return e.getClass().getSimpleName()
                + ": "
                + e.getMessage();
    }
}