//src/main/java/com/fitroute/domain/plan/service/DailyPlanService.java
package com.fitroute.domain.plan.service;

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
    private final AiPlanService aiPlanService;

    // ─────────────────────────────────────────────
    // 오늘 플랜 생성
    // ─────────────────────────────────────────────
    @Transactional
    public DailyPlanResponse generateTodayPlan(Long userId) {

        LocalDate today = LocalDate.now();

        // 1. 이미 존재하면 반환
        Optional<DailyPlan> existing = dailyPlanRepository.findByUserIdAndPlanDate(userId, today);

        if (existing.isPresent()) {
            log.info("[DailyPlan] Already exists - userId={}, date={}", userId, today);
            return DailyPlanResponse.from(existing.get());
        }

        // 2. 사용자 프로필 조회
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.PROFILE_NOT_FOUND.getMessage()));

        // 3. 목표 칼로리 계산
        int calorieTarget = calculateCalorieTarget(profile);

        // 4. AI 호출
        Map<String, Object> aiResult = aiPlanService.generateDailyPlan(profile, calorieTarget);

        Map<String, Object> mealPlan = extractMap(aiResult, "meal_plan");
        Map<String, Object> workoutPlan = extractMap(aiResult, "workout_plan");

        // 5. DailyPlan 생성 (Builder 최소화)
        DailyPlan plan = DailyPlan.builder()
                .userId(userId)
                .planDate(today)
                .build();

        // 6. AI 결과 반영 (핵심)
        plan.complete(
                calorieTarget,
                mealPlan,
                workoutPlan,
                Map.of(
                        "model", "gemini-2.5-flash-lite",
                        "generated_at", today.toString()));

        try {
            // 7. 저장
            DailyPlan saved = dailyPlanRepository.save(plan);

            log.info("[DailyPlan] Generated - userId={}, date={}, kcal={}",
                    userId, today, calorieTarget);

            // 8. PlanItem 생성
            savePlanItems(saved, mealPlan, workoutPlan, today);

            return DailyPlanResponse.from(saved);

        } catch (DataIntegrityViolationException e) {
            log.warn("[DailyPlan] Race condition - userId={}, date={}", userId, today);

            return dailyPlanRepository.findByUserIdAndPlanDate(userId, today)
                    .map(DailyPlanResponse::from)
                    .orElseThrow(() -> new IllegalStateException("플랜 저장 후 조회 실패"));
        }
    }

    // ─────────────────────────────────────────────
    // 오늘 플랜 조회
    // ─────────────────────────────────────────────
    public DailyPlanResponse getTodayPlan(Long userId) {
        return dailyPlanRepository
                .findByUserIdAndPlanDate(userId, LocalDate.now())
                .map(DailyPlanResponse::from)
                .orElse(DailyPlanResponse.builder().status("NO_PLAN").build());
    }

    // ─────────────────────────────────────────────
    // PlanItem 저장
    // ─────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void savePlanItems(DailyPlan dailyPlan,
            Map<String, Object> mealPlan,
            Map<String, Object> workoutPlan,
            LocalDate date) {

        List<PlanItem> items = new ArrayList<>();

        // 식단
        if (mealPlan != null) {
            Object mealsObj = mealPlan.get("meals");

            if (mealsObj instanceof List<?> meals) {
                for (Object obj : meals) {
                    if (!(obj instanceof Map))
                        continue;

                    Map<String, Object> meal = (Map<String, Object>) obj;

                    String typeStr = String.valueOf(meal.getOrDefault("type", "SNACK"));
                    String name = String.valueOf(meal.getOrDefault("name", "식사"));
                    int kcal = toInt(meal.get("kcal"), 0);

                    int protein = toInt(meal.get("protein"), 0);
                    int carbs = toInt(meal.get("carbs"), 0);
                    int fat = toInt(meal.get("fat"), 0);

                    items.add(PlanItem.builder()
                            .dailyPlan(dailyPlan)
                            .date(date)
                            .type(PlanItemType.MEAL)
                            .category(parseMealCategory(typeStr))
                            .foodName(name)
                            .calories(kcal)
                            .protein(protein)
                            .carbs(carbs)
                            .fat(fat)
                            .status(PlanItemStatus.PENDING)
                            .build());
                }
            }
        }

        // 운동
        if (workoutPlan != null) {
            Object workoutsObj = workoutPlan.get("workouts");

            if (workoutsObj instanceof List<?> workouts) {
                for (Object obj : workouts) {
                    if (!(obj instanceof Map))
                        continue;

                    Map<String, Object> workout = (Map<String, Object>) obj;

                    String categoryStr = String.valueOf(workout.getOrDefault("category", "CARDIO"));
                    int sets = toInt(workout.get("sets"), 0);
                    int reps = toInt(workout.get("reps"), 0);

                    String name = String.valueOf(workout.getOrDefault("name", "운동"));
                    int kcalBurn = toInt(workout.get("kcal_burn"), 0);
                    int durationMin = toInt(workout.get("duration_min"), 0);

                    items.add(PlanItem.builder()
                            .dailyPlan(dailyPlan)
                            .date(date)
                            .type(PlanItemType.WORKOUT)
                            .category(parseWorkoutCategory(categoryStr))
                            .exerciseName(name)
                            .calories(kcalBurn)
                            .sets(sets)
                            .reps(reps)
                            .durationMin(durationMin)
                            .status(PlanItemStatus.PENDING)
                            .build());
                }
            }
        }

        if (items.isEmpty()) {
            log.warn("[PlanItem] 저장할 아이템 없음 - planId={}", dailyPlan.getId());
            return;
        }

        planItemRepository.saveAll(items);
        log.info("[PlanItem] {} items saved - planId={}", items.size(), dailyPlan.getId());
    }

    // ─────────────────────────────────────────────
    // PlanItem 추가 (운동/식단)
    // ─────────────────────────────────────────────
    @Transactional
    public PlanItemResponse addPlanItem(Long userId, PlanItemCreateRequest req) {
        req.validate();

        LocalDate today = LocalDate.now();

        // 1. 오늘의 DailyPlan 조회
        DailyPlan dailyPlan = dailyPlanRepository
                .findByUserIdAndPlanDate(userId, today)
                .orElseGet(() -> {
                    // DailyPlan이 없으면 기본값으로 새로 생성
                    DailyPlan newPlan = DailyPlan.builder()
                            .userId(userId)
                            .planDate(today)
                            .build();
                    // 상태를 ACTIVE로 설정 (완성된 상태)
                    newPlan.complete(
                            1500, // 기본 칼로리 목표
                            new HashMap<>(),
                            new HashMap<>(),
                            new HashMap<>());
                    return dailyPlanRepository.save(newPlan);
                });

        // 2. Status 결정 (요청에서 받거나 기본값 사용)
        PlanItemStatus itemStatus = req.getStatus() != null
                ? req.getStatus()
                : PlanItemStatus.COMPLETED;

        // 3. PlanItem 생성
        PlanItem item = PlanItem.builder()
                .dailyPlan(dailyPlan)
                .date(today)
                .type(req.getType())
                .category(req.getCategory())
                .calories(req.getCalories())
                .status(itemStatus)
                .build();

        // 4. 타입별로 필드 설정
        if (req.getType() == PlanItemType.MEAL) {
            item.setFoodName(req.getName());
            item.setProtein(req.getProtein());
            item.setCarbs(req.getCarbs());
            item.setFat(req.getFat());
        } else if (req.getType() == PlanItemType.WORKOUT) {
            item.setExerciseName(req.getName());
            item.setSets(req.getSets());
            item.setReps(req.getReps());
            item.setWeightKg(req.getWeightKg());
            item.setDurationMin(req.getDurationMin());
        }

        // 5. 저장
        PlanItem saved = planItemRepository.save(item);

        log.info("[PlanItem] Added - userId={}, type={}, name={}, status={}, date={}",
                userId, req.getType(), req.getName(), itemStatus, today);

        return PlanItemResponse.from(saved);
    }

    // ─────────────────────────────────────────────
    // 주간 운동 계획 조회
    // ─────────────────────────────────────────────
    public List<WeeklyWorkoutPlanResponse> getWeeklyWorkoutPlan(Long userId, LocalDate startDate) {
        // 1. startDate부터 7일간의 DailyPlan 조회
        LocalDate endDate = startDate.plusDays(6);

        List<DailyPlan> dailyPlans = dailyPlanRepository
                .findByUserIdAndPlanDateBetween(userId, startDate, endDate);

        if (dailyPlans.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. DailyPlan별로 PlanItem 조회 (WORKOUT만)
        List<WeeklyWorkoutPlanResponse> weeklyPlans = new ArrayList<>();

        for (DailyPlan plan : dailyPlans) {
            List<PlanItem> workoutItems = planItemRepository
                    .findByPlanIdAndDateAndType(plan.getId(), plan.getPlanDate(), PlanItemType.WORKOUT);

            // 3. PlanItem을 RoutineBlock으로 변환
            Map<PlanItemCategory, List<PlanItem>> grouped = workoutItems.stream()
                    .collect(Collectors.groupingBy(PlanItem::getCategory));

            List<WeeklyWorkoutPlanResponse.RoutineBlockDto> routines = grouped.entrySet().stream()
                    .map(entry -> {
                        PlanItemCategory cat = entry.getKey();
                        List<PlanItem> items = entry.getValue();

                        // 카테고리 내 총 칼로리, 총 시간 합산
                        int totalCalories = items.stream()
                                .mapToInt(PlanItem::getEffectiveCalories).sum();
                        int totalDuration = items.stream()
                                .mapToInt(i -> i.getDurationMin() != null ? i.getDurationMin() : 0).sum();

                        // 카테고리 내 운동들을 ExerciseDto로 변환
                        List<WeeklyWorkoutPlanResponse.RoutineBlockDto.ExerciseDto> exercises = items.stream()
                                .map(i -> WeeklyWorkoutPlanResponse.RoutineBlockDto.ExerciseDto.builder()
                                        .name(i.getEffectiveName())
                                        .sets(i.getEffectiveSets())
                                        .reps(i.getEffectiveReps())
                                        .build())
                                .collect(Collectors.toList());

                        return WeeklyWorkoutPlanResponse.RoutineBlockDto.builder()
                                .id((long) cat.ordinal()) // 카테고리 기반 id
                                .name(getCategoryName(cat)) // "가슴 루틴", "유산소 루틴"
                                .duration(totalDuration)
                                .category(cat.name())
                                .calories(totalCalories)
                                .exercises(exercises)
                                .build();
                    })
                    .collect(Collectors.toList());

            // 4. 요일명 계산
            String dayName = getDayName(plan.getPlanDate());

            // 5. 응답 빌드
            WeeklyWorkoutPlanResponse response = WeeklyWorkoutPlanResponse.builder()
                    .date(plan.getPlanDate())
                    .dayName(dayName)
                    .routines(routines)
                    .build();

            weeklyPlans.add(response);
        }

        return weeklyPlans;
    }

    // 요일명 반환 헬퍼
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

    // ─────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────
    private PlanItemCategory parseMealCategory(String type) {
        return switch (type.toUpperCase()) {
            case "BREAKFAST" -> PlanItemCategory.BREAKFAST;
            case "LUNCH" -> PlanItemCategory.LUNCH;
            case "DINNER" -> PlanItemCategory.DINNER;
            default -> PlanItemCategory.SNACK;
        };
    }

    private PlanItemCategory parseWorkoutCategory(String name) {
        if (name == null)
            return PlanItemCategory.CHEST;

        String lower = name.toLowerCase();

        if (lower.contains("hiit") || lower.contains("러닝") || lower.contains("cardio"))
            return PlanItemCategory.CARDIO;

        if (lower.contains("코어") || lower.contains("core"))
            return PlanItemCategory.CORE;

        if (lower.contains("하체") || lower.contains("leg"))
            return PlanItemCategory.LEGS;

        if (lower.contains("등") || lower.contains("back"))
            return PlanItemCategory.BACK;

        if (lower.contains("어깨") || lower.contains("shoulder"))
            return PlanItemCategory.SHOULDERS;

        if (lower.contains("팔") || lower.contains("arm"))
            return PlanItemCategory.ARMS;

        return PlanItemCategory.CHEST;
    }

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

    private int toInt(Object value, int defaultVal) {
        if (value == null)
            return defaultVal;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return defaultVal;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        log.warn("[DailyPlan] '{}' 없음 → 빈 Map", key);
        return Collections.emptyMap();
    }

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
                    case "MAINTENANCE" -> 1.0;
                    case "MUSCLE_GAIN" -> 1.1;
                    default -> 0.8;
                }
                : 0.8;

        return (int) Math.round(tdee * adjust);
    }
}