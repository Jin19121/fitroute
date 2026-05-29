// domain/plan/service/DailyPlanService.java
package com.fitroute.domain.plan.service;

import com.fitroute.domain.food.entity.Food;
import com.fitroute.domain.food.repository.FoodRepository;
import com.fitroute.domain.log.service.LogService;
import com.fitroute.domain.plan.dto.DailyPlanResponse;
import com.fitroute.domain.plan.dto.PlanItemCreateRequest;
import com.fitroute.domain.plan.dto.PlanItemResponse;
import com.fitroute.domain.plan.dto.WeeklyWorkoutPlanResponse;
import com.fitroute.domain.plan.entity.DailyPlan;
import com.fitroute.domain.plan.entity.PlanItem;
import com.fitroute.domain.plan.repository.DailyPlanRepository;
import com.fitroute.domain.plan.repository.PlanItemRepository;
import com.fitroute.domain.user.entity.UserProfile;
import com.fitroute.domain.user.repository.UserProfileRepository;
import com.fitroute.global.ai.GeminiClient;
import com.fitroute.global.ai.GeminiPromptBuilder;
import com.fitroute.global.ai.GeminiResponseParser;
import com.fitroute.global.enums.PlanItemCategory;
import com.fitroute.global.enums.PlanItemStatus;
import com.fitroute.global.enums.PlanItemType;
import com.fitroute.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyPlanService {

    private final DailyPlanRepository dailyPlanRepository;
    private final PlanItemRepository planItemRepository;
    private final UserProfileRepository userProfileRepository;
    private final LogService logService;
    private final FoodRepository foodRepository;
    private final GeminiClient geminiClient;
    private final GeminiResponseParser responseParser;
    private final GeminiPromptBuilder promptBuilder;

    // ─────────────────────────────────────────────────────────────────────
    // 오늘 플랜 생성
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public DailyPlanResponse generateTodayPlan(Long userId) {
        LocalDate today = LocalDate.now();

        // ACTIVE 플랜 이미 존재 시 반환
        Optional<DailyPlan> activePlan = dailyPlanRepository
                .findByUserIdAndPlanDateAndStatus(userId, today, DailyPlan.PlanStatus.ACTIVE);
        if (activePlan.isPresent()) {
            log.info("[DailyPlan] Already ACTIVE - userId={}, date={}, v{}",
                    userId, today, activePlan.get().getVersion());
            return DailyPlanResponse.from(activePlan.get());
        }

        // 버전 계산
        Optional<DailyPlan> latestOpt = dailyPlanRepository
                .findTopByUserIdAndPlanDateOrderByVersionDesc(userId, today);
        int newVersion = latestOpt.map(p -> p.getVersion() + 1).orElse(1);
        Long rootPlanId = latestOpt.map(DailyPlan::getRootPlanId).orElse(null);
        latestOpt.filter(p -> p.getStatus() != DailyPlan.PlanStatus.SUPERSEDED)
                .ifPresent(DailyPlan::supersede);

        // 사용자 프로필 + 칼로리 목표
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        ErrorCode.PROFILE_NOT_FOUND.getMessage()));
        int calorieTarget = calculateCalorieTarget(profile);

        // DailyPlan 생성 (저장은 complete 후 한 번만)
        DailyPlan plan = DailyPlan.builder()
                .userId(userId)
                .planDate(today)
                .version(newVersion)
                .rootPlanId(rootPlanId)
                .build();

        // ── 식단: DB 기반 선택 ────────────────────────────────────────────
        List<Food> availableFoods = buildMenuForUser(profile);
        List<PlanItem> mealItems = generateMealItems(
                profile, plan, today, calorieTarget, availableFoods);

        // ── 운동: AI 자유 생성 ────────────────────────────────────────────
        String workoutRaw = geminiClient.call(
                promptBuilder.getPlanSystemInstruction(),
                promptBuilder.buildWorkoutPrompt(profile));
        String workoutJson = responseParser.extractText(workoutRaw);
        Map<String, Object> workoutPlan = responseParser.parseWorkoutPlan(workoutJson);

        // ── complete → 저장 ───────────────────────────────────────────────
        plan.complete(
                calorieTarget,
                buildMealPlanJson(mealItems),
                        workoutPlan,
                Map.of("mealSource", "DB", "workoutSource", "AI", "version", newVersion));

        try {
            DailyPlan saved = dailyPlanRepository.save(plan);

            // 운동 PlanItem 파싱 + 저장 (saved 확정 후 실행)
            List<PlanItem> workoutItems = responseParser.parsePlanItems(workoutJson, saved);

            // 식단 + 운동 한 번에 저장
            List<PlanItem> allItems = new ArrayList<>(mealItems);
            allItems.addAll(workoutItems);
            if (!allItems.isEmpty()) {
                planItemRepository.saveAll(allItems);
            }

            logService.createForPlan(saved);

            log.info("[DailyPlan] Generated v{} - userId={}, date={}, kcal={}, meal={}개, workout={}개",
                    newVersion, userId, today, calorieTarget, mealItems.size(), workoutItems.size());

            return DailyPlanResponse.from(saved);

        } catch (DataIntegrityViolationException e) {
            log.warn("[DailyPlan] Race condition - userId={}, date={}", userId, today);
            return dailyPlanRepository
                    .findByUserIdAndPlanDateAndStatus(userId, today, DailyPlan.PlanStatus.ACTIVE)
                    .map(DailyPlanResponse::from)
                    .orElseThrow(() -> new IllegalStateException("플랜 저장 후 조회 실패"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 오늘 플랜 조회
    // ─────────────────────────────────────────────────────────────────────

    public DailyPlanResponse getTodayPlan(Long userId) {
        return dailyPlanRepository
                .findByUserIdAndPlanDateAndStatus(userId, LocalDate.now(), DailyPlan.PlanStatus.ACTIVE)
                .map(DailyPlanResponse::from)
                .orElse(DailyPlanResponse.builder().status("NO_PLAN").build());
    }

    // ─────────────────────────────────────────────────────────────────────
    // 식단 스타일별 메뉴 조회
    // ─────────────────────────────────────────────────────────────────────

    private List<Food> buildMenuForUser(UserProfile profile) {
        String tag = switch (profile.getDietStyle()) {
            case LOW_CALORIE -> "저칼로리";
            case LOW_CARB_HIGH_PROTEIN -> "고단백";
            default -> "";
        };

        if (tag.isBlank())
            return foodRepository.findAll();

        List<Food> tagged = foodRepository.findAllByTagsContaining(tag);
        List<Food> general = foodRepository.findAll().stream()
                .filter(f -> !tagged.contains(f))
                .limit(20)
                .toList();

        List<Food> combined = new ArrayList<>(tagged);
        combined.addAll(general);
        return combined;
    }

    // ─────────────────────────────────────────────────────────────────────
    // DB 기반 식단 PlanItem 생성
    // ─────────────────────────────────────────────────────────────────────

    private List<PlanItem> generateMealItems(
            UserProfile profile,
            DailyPlan dailyPlan,
            LocalDate date,
            int calorieTarget,
            List<Food> availableFoods) {

        if (availableFoods.isEmpty()) {
            log.warn("[DailyPlan] foods 테이블 비어있음 → 식단 생략");
            return List.of();
        }

        String prompt = promptBuilder.buildMealSelectionPrompt(profile, availableFoods, calorieTarget);
        String rawResponse = geminiClient.call(getMealSystemInstruction(), prompt);
        String planJson = responseParser.extractText(rawResponse);

        List<GeminiResponseParser.SelectedMealDto> selected = responseParser.parseMealSelection(planJson);

        if (selected.isEmpty()) {
            log.warn("[DailyPlan] AI 식단 선택 실패 → fallback 사용");
            return buildFallbackMealItems(availableFoods, dailyPlan, date);
        }

        Map<Long, Food> foodMap = foodRepository
                .findByIdIn(selected.stream()
                        .map(GeminiResponseParser.SelectedMealDto::foodId).toList())
                .stream()
                .collect(Collectors.toMap(Food::getId, f -> f));

        return selected.stream().map(s -> {
            Food food = foodMap.get(s.foodId());
            if (food == null) {
                log.warn("[DailyPlan] AI가 선택한 foodId={} DB에 없음 → 스킵", s.foodId());
                return null;
            }
            PlanItemCategory category = switch (s.mealType()) {
                case "BREAKFAST" -> PlanItemCategory.BREAKFAST;
                case "LUNCH" -> PlanItemCategory.LUNCH;
                case "DINNER" -> PlanItemCategory.DINNER;
                default -> PlanItemCategory.SNACK;
            };
            return PlanItem.builder()
                    .dailyPlan(dailyPlan)
                    .date(date)
                    .type(PlanItemType.MEAL)
                    .category(category)
                    .foodName(food.getName())
                    .calories(food.getCalories()) // DB 확정값
                    .protein(food.getProtein()) // DB 확정값
                    .fat(food.getFat()) // DB 확정값
                    .carbs(food.getCarbs()) // DB 확정값
                    .status(PlanItemStatus.PENDING)
                    .build();
        }).filter(Objects::nonNull).toList();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Fallback 식단
    // ─────────────────────────────────────────────────────────────────────

    private List<PlanItem> buildFallbackMealItems(
            List<Food> foods, DailyPlan dailyPlan, LocalDate date) {

        List<PlanItem> items = new ArrayList<>();
        for (PlanItemCategory cat : List.of(
                PlanItemCategory.BREAKFAST, PlanItemCategory.LUNCH, PlanItemCategory.DINNER)) {
            foods.stream().filter(f -> f.getCategory() == cat).findFirst()
                    .ifPresent(food -> items.add(PlanItem.builder()
                            .dailyPlan(dailyPlan).date(date)
                            .type(PlanItemType.MEAL).category(cat)
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

    // ─────────────────────────────────────────────────────────────────────
    // mealPlan JSON 구성
    // ─────────────────────────────────────────────────────────────────────

    private Map<String, Object> buildMealPlanJson(List<PlanItem> mealItems) {
        List<Map<String, Object>> meals = mealItems.stream()
                .map(item -> Map.<String, Object>of(
                        "type", item.getCategory().name(),
                        "name", item.getFoodName(),
                        "kcal", item.getCalories(),
                        "protein", item.getProtein(),
                        "carbs", item.getCarbs(),
                        "fat", item.getFat()))
                .toList();
        int totalKcal = mealItems.stream().mapToInt(PlanItem::getCalories).sum();
        return Map.of("meals", meals, "total_kcal", totalKcal);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 식단 AI System Instruction
    // ─────────────────────────────────────────────────────────────────────

    private String getMealSystemInstruction() {
        return """
                Role: Nutritionist AI for FitRoute.
                Output: JSON ONLY. No markdown, no explanation.
                Task: Select food IDs to compose 3 meals within the calorie target.
                Schema: {"meals":[{"id":int,"meal_type":"BREAKFAST|LUNCH|DINNER"}]}

                Critical Rules:
                - 총 선택 음식 수: 6~9개 (끼니당 1~4개)
                - 각 끼니 칼로리 합이 지정된 목표 ±100 이내 — 이 규칙이 가장 중요
                - 전체 칼로리 절대 초과 금지
                - "생것", "말린것", "살코기", "분말" 포함 음식 선택 금지
                """;
    }

    // ─────────────────────────────────────────────────────────────────────
    // PlanItem 직접 추가
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public PlanItemResponse addPlanItem(Long userId, PlanItemCreateRequest req) {
        req.validate();
        LocalDate today = LocalDate.now();

        DailyPlan dailyPlan = dailyPlanRepository
                .findByUserIdAndPlanDateAndStatus(userId, today, DailyPlan.PlanStatus.ACTIVE)
                .orElseGet(() -> {
                    DailyPlan newPlan = DailyPlan.builder()
                            .userId(userId).planDate(today).build();
                    newPlan.complete(1500, new HashMap<>(), new HashMap<>(), new HashMap<>());
                    DailyPlan saved = dailyPlanRepository.save(newPlan);
                    logService.createForPlan(saved);
                    return saved;
                });

        PlanItemStatus itemStatus = req.getStatus() != null
                ? req.getStatus()
                : PlanItemStatus.COMPLETED;

        PlanItem item = PlanItem.builder()
                .dailyPlan(dailyPlan).date(today)
                .type(req.getType()).category(req.getCategory())
                .calories(req.getCalories()).status(itemStatus)
                .build();

        if (req.getType() == PlanItemType.MEAL) {
            item.setFoodName(req.getName());
            item.setProtein(req.getProtein());
            item.setCarbs(req.getCarbs());
            item.setFat(req.getFat());
        } else {
            item.setExerciseName(req.getName());
            item.setSets(req.getSets());
            item.setReps(req.getReps());
            item.setWeightKg(req.getWeightKg());
            item.setDurationMin(req.getDurationMin());
        }

        PlanItem saved = planItemRepository.save(item);
        if (itemStatus != PlanItemStatus.PENDING) {
            logService.upsertFromPlanItem(saved);
        }
        return PlanItemResponse.from(saved);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 주간 운동 계획 조회
    // ─────────────────────────────────────────────────────────────────────

    public List<WeeklyWorkoutPlanResponse> getWeeklyWorkoutPlan(
            Long userId, LocalDate startDate) {

        LocalDate endDate = startDate.plusDays(6);
        List<DailyPlan> dailyPlans = dailyPlanRepository
                .findByUserIdAndPlanDateBetween(userId, startDate, endDate);

        if (dailyPlans.isEmpty())
            return new ArrayList<>();

        List<WeeklyWorkoutPlanResponse> weeklyPlans = new ArrayList<>();
        for (DailyPlan plan : dailyPlans) {
            List<PlanItem> workoutItems = planItemRepository.findByPlanIdAndDateAndType(
                    plan.getId(), plan.getPlanDate(), PlanItemType.WORKOUT);

            Map<PlanItemCategory, List<PlanItem>> grouped = workoutItems.stream()
                    .collect(Collectors.groupingBy(PlanItem::getCategory));

            List<WeeklyWorkoutPlanResponse.RoutineBlockDto> routines = grouped.entrySet().stream()
                    .map(entry -> {
                        List<PlanItem> its = entry.getValue();
                        int totalCalories = its.stream().mapToInt(PlanItem::getEffectiveCalories).sum();
                        int totalDuration = its.stream()
                                .mapToInt(i -> i.getDurationMin() != null ? i.getDurationMin() : 0).sum();
                        List<WeeklyWorkoutPlanResponse.RoutineBlockDto.ExerciseDto> exercises = its.stream()
                                .map(i -> WeeklyWorkoutPlanResponse.RoutineBlockDto.ExerciseDto.builder()
                                        .name(i.getEffectiveName())
                                        .sets(i.getEffectiveSets())
                                        .reps(i.getEffectiveReps())
                                        .build())
                                .collect(Collectors.toList());
                        return WeeklyWorkoutPlanResponse.RoutineBlockDto.builder()
                                .id((long) entry.getKey().ordinal())
                                .name(getCategoryName(entry.getKey()))
                                .duration(totalDuration)
                                .category(entry.getKey().name())
                                .calories(totalCalories)
                                .exercises(exercises)
                                .build();
                    })
                    .collect(Collectors.toList());

            weeklyPlans.add(WeeklyWorkoutPlanResponse.builder()
                    .date(plan.getPlanDate())
                    .dayName(getDayName(plan.getPlanDate()))
                    .routines(routines)
                    .build());
        }
        return weeklyPlans;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private String getCategoryName(PlanItemCategory category) {
        return switch (category) {
            case CHEST -> "가슴 루틴";
            case BACK -> "등 루틴";
            case LEGS -> "하체 루틴";
            case SHOULDERS -> "어깨 루틴";
            case ARMS -> "팔 루틴";
            case CORE -> "코어 루틴";
            case CARDIO -> "유산소 루틴";
            case REST -> "휴식";
            default -> category.name();
        };
    }

    private String getDayName(LocalDate date) {
        return switch (date.getDayOfWeek().getValue()) {
            case 1 -> "월";
            case 2 -> "화";
            case 3 -> "수";
            case 4 -> "목";
            case 5 -> "금";
            case 6 -> "토";
            case 7 -> "일";
            default -> "요일";
        };
    }

    // private int toInt(Object value, int defaultVal) {
    // if (value == null)
    // return defaultVal;
    // try {
    // return Integer.parseInt(String.valueOf(value));
    // } catch (Exception e) {
    // return defaultVal;
    // }
    // }

    // @SuppressWarnings("unchecked")
    // private Map<String, Object> extractMap(Map<String, Object> source, String
    // key) {
    // Object value = source.get(key);
    // return (value instanceof Map<?, ?>) ? (Map<String, Object>) value :
    // Collections.emptyMap();
    // }

    private int calculateCalorieTarget(UserProfile profile) {
        if (profile.getWeight() == null || profile.getHeight() == null)
            return 1500;

        int age = 30;
        if (profile.getBirthDate() != null) {
            age = LocalDate.now().getYear() - profile.getBirthDate().getYear();
        }

        double bmr = ("MALE".equals(profile.getGender().name()))
                ? 10 * profile.getWeight() + 6.25 * profile.getHeight() - 5 * age + 5
                : 10 * profile.getWeight() + 6.25 * profile.getHeight() - 5 * age - 161;

        double activity = profile.getActivityLevel() != null
                ? profile.getActivityLevel().getMultiplier()
                : 1.375;

        double tdee = bmr * activity;

        double adjust = profile.getGoalType() != null
                ? switch (profile.getGoalType().name()) {
                    case "WEIGHT_LOSS" -> 0.8;
                    case "MUSCLE_GAIN" -> 1.1;
                    default -> 1.0;
                }
                : 0.8;

        return (int) Math.round(tdee * adjust);
    }
}