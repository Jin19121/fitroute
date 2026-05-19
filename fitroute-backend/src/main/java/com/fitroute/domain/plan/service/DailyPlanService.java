// domain/plan/service/DailyPlanService.java
package com.fitroute.domain.plan.service;

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
    private final LogService logService;

    // ─────────────────────────────────────────────────────────────────────
    // 오늘 플랜 생성 (버전 관리 포함)
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public DailyPlanResponse generateTodayPlan(Long userId) {
        LocalDate today = LocalDate.now();

        // 1. ACTIVE 플랜이 이미 있으면 반환 (idempotent)
        Optional<DailyPlan> activePlan = dailyPlanRepository
                .findByUserIdAndPlanDateAndStatus(userId, today, DailyPlan.PlanStatus.ACTIVE);
        if (activePlan.isPresent()) {
            log.info("[DailyPlan] Already ACTIVE - userId={}, date={}, v{}",
                    userId, today, activePlan.get().getVersion());
            return DailyPlanResponse.from(activePlan.get());
        }

        // 2. 최신 버전 조회 → versioning 정보 계산
        Optional<DailyPlan> latestOpt = dailyPlanRepository
                .findTopByUserIdAndPlanDateOrderByVersionDesc(userId, today);

        int newVersion = 1;
        Long rootPlanId = null;

        if (latestOpt.isPresent()) {
            DailyPlan latest = latestOpt.get();
            newVersion = latest.getVersion() + 1;
            rootPlanId = latest.getRootPlanId();

            // GENERATING/FAILED/SUPERSEDED 상태 플랜은 SUPERSEDE 처리
            if (latest.getStatus() != DailyPlan.PlanStatus.SUPERSEDED) {
                latest.supersede();
            }
        }

        // 3. 사용자 프로필 + 칼로리 목표
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        ErrorCode.PROFILE_NOT_FOUND.getMessage()));
        int calorieTarget = calculateCalorieTarget(profile);

        // 4. AI 호출
        Map<String, Object> aiResult = aiPlanService.generateDailyPlan(profile, calorieTarget);
        Map<String, Object> mealPlan = extractMap(aiResult, "meal_plan");
        Map<String, Object> workoutPlan = extractMap(aiResult, "workout_plan");

        // 5. DailyPlan 생성 (버전 포함)
        DailyPlan plan = DailyPlan.builder()
                .userId(userId)
                .planDate(today)
                .version(newVersion)
                .rootPlanId(rootPlanId)
                .build();

        plan.complete(
                calorieTarget,
                mealPlan,
                workoutPlan,
                Map.of("model", "gemini-2.5-flash-lite",
                        "generated_at", today.toString(),
                        "version", newVersion));

        try {
            DailyPlan saved = dailyPlanRepository.save(plan);
            log.info("[DailyPlan] Generated v{} - userId={}, date={}, kcal={}",
                    newVersion, userId, today, calorieTarget);

            savePlanItems(saved, mealPlan, workoutPlan, today);

            // ★ PHASE 3: 빈 Log 생성
            logService.createForPlan(saved);

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
    // PlanItem 저장 (기존 로직 유지)
    // ─────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void savePlanItems(DailyPlan dailyPlan,
            Map<String, Object> mealPlan,
            Map<String, Object> workoutPlan,
            LocalDate date) {
        List<PlanItem> items = new ArrayList<>();

        if (mealPlan != null) {
            Object mealsObj = mealPlan.get("meals");
            if (mealsObj instanceof List<?> meals) {
                for (Object obj : meals) {
                    if (!(obj instanceof Map))
                        continue;
                    Map<String, Object> meal = (Map<String, Object>) obj;
                    items.add(PlanItem.builder()
                            .dailyPlan(dailyPlan)
                            .date(date)
                            .type(PlanItemType.MEAL)
                            .category(parseMealCategory(String.valueOf(meal.getOrDefault("type", "SNACK"))))
                            .foodName(String.valueOf(meal.getOrDefault("name", "식사")))
                            .calories(toInt(meal.get("kcal"), 0))
                            .protein(toInt(meal.get("protein"), 0))
                            .carbs(toInt(meal.get("carbs"), 0))
                            .fat(toInt(meal.get("fat"), 0))
                            .status(PlanItemStatus.PENDING)
                            .build());
                }
            }
        }

        if (workoutPlan != null) {
            Object workoutsObj = workoutPlan.get("workouts");
            if (workoutsObj instanceof List<?> workouts) {
                for (Object obj : workouts) {
                    if (!(obj instanceof Map))
                        continue;
                    Map<String, Object> workout = (Map<String, Object>) obj;
                    items.add(PlanItem.builder()
                            .dailyPlan(dailyPlan)
                            .date(date)
                            .type(PlanItemType.WORKOUT)
                            .category(parseWorkoutCategory(String.valueOf(workout.getOrDefault("category", "CARDIO"))))
                            .exerciseName(String.valueOf(workout.getOrDefault("name", "운동")))
                            .calories(toInt(workout.get("kcal_burn"), 0))
                            .sets(toInt(workout.get("sets"), 0))
                            .reps(toInt(workout.get("reps"), 0))
                            .durationMin(toInt(workout.get("duration_min"), 0))
                            .status(PlanItemStatus.PENDING)
                            .build());
                }
            }
        }

        if (!items.isEmpty()) {
            planItemRepository.saveAll(items);
            log.info("[PlanItem] {} items saved - planId={}", items.size(), dailyPlan.getId());
        }
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
                .dailyPlan(dailyPlan)
                .date(today)
                .type(req.getType())
                .category(req.getCategory())
                .calories(req.getCalories())
                .status(itemStatus)
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

        // 직접 추가한 아이템도 Log에 반영
        if (itemStatus != PlanItemStatus.PENDING) {
            logService.upsertFromPlanItem(saved);
        }

        return PlanItemResponse.from(saved);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 주간 운동 계획 조회 (기존 로직 유지)
    // ─────────────────────────────────────────────────────────────────────

    public List<WeeklyWorkoutPlanResponse> getWeeklyWorkoutPlan(Long userId, LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(6);
        List<DailyPlan> dailyPlans = dailyPlanRepository
                .findByUserIdAndPlanDateBetween(userId, startDate, endDate);

        if (dailyPlans.isEmpty())
            return new ArrayList<>();

        List<WeeklyWorkoutPlanResponse> weeklyPlans = new ArrayList<>();
        for (DailyPlan plan : dailyPlans) {
            List<PlanItem> workoutItems = planItemRepository
                    .findByPlanIdAndDateAndType(plan.getId(), plan.getPlanDate(), PlanItemType.WORKOUT);

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
    // 헬퍼
    // ─────────────────────────────────────────────────────────────────────

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
        return (value instanceof Map<?, ?>) ? (Map<String, Object>) value : Collections.emptyMap();
    }

    private int calculateCalorieTarget(UserProfile profile) {
        if (profile.getWeight() == null || profile.getHeight() == null)
            return 1500;
        int age = 30;
        if (profile.getBirthDate() != null)
            age = LocalDate.now().getYear() - profile.getBirthDate().getYear();
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