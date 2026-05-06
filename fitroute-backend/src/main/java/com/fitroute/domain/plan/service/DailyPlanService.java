package com.fitroute.domain.plan.service;

import com.fitroute.domain.plan.dto.DailyPlanResponse;
import com.fitroute.domain.plan.entity.DailyPlan;
import com.fitroute.domain.plan.repository.DailyPlanRepository;
import com.fitroute.domain.user.entity.UserProfile;
import com.fitroute.domain.user.repository.UserProfileRepository;
import com.fitroute.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyPlanService {

    private final DailyPlanRepository dailyPlanRepository;
    private final UserProfileRepository userProfileRepository;
    private final AiPlanService aiPlanService;

    // DailyPlanService.java — generateTodayPlan 메서드만 교체
    @Transactional
    public DailyPlanResponse generateTodayPlan(Long userId) {
        LocalDate today = LocalDate.now();

        // 이미 있으면 바로 반환
        Optional<DailyPlan> existing = dailyPlanRepository.findByUserIdAndPlanDate(userId, today);
        if (existing.isPresent()) {
            return DailyPlanResponse.from(existing.get());
        }

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        ErrorCode.PROFILE_NOT_FOUND.getMessage()));

        int calorieTarget = calculateCalorieTarget(profile);
        Map<String, Object> aiResult = aiPlanService.generateDailyPlan(profile, calorieTarget);

        Map<String, Object> mealPlan = extractMap(aiResult, "meal_plan");
        Map<String, Object> workoutPlan = extractMap(aiResult, "workout_plan");

        DailyPlan plan = DailyPlan.builder()
                .userId(userId)
                .planDate(today)
                .calorieTarget(calorieTarget)
                .mealPlan(mealPlan)
                .workoutPlan(workoutPlan)
                .aiMeta(Map.of("model", "gemini-2.5-flash-lite",
                        "generated_at", today.toString()))
                .build();

        try {
            DailyPlan saved = dailyPlanRepository.save(plan);
            log.info("[DailyPlan] Generated - userId={}, date={}, kcal={}", userId, today, calorieTarget);
            return DailyPlanResponse.from(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 동시 요청으로 race condition 발생 시 — 이미 저장된 레코드를 반환
            log.warn("[DailyPlan] Race condition detected for userId={}, date={}. Returning existing.", userId, today);
            return dailyPlanRepository.findByUserIdAndPlanDate(userId, today)
                    .map(DailyPlanResponse::from)
                    .orElseThrow(() -> new IllegalStateException("플랜 저장 후 조회 실패"));
        }
    }

    public DailyPlanResponse getTodayPlan(Long userId) {
        return dailyPlanRepository.findByUserIdAndPlanDate(userId, LocalDate.now())
                .map(DailyPlanResponse::from)
                .orElse(DailyPlanResponse.builder()
                        .status("NO_PLAN")
                        .build());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        log.warn("[DailyPlan] AI 응답에서 '{}' 키를 찾을 수 없거나 타입 불일치. 빈 Map으로 대체.", key);
        return Collections.emptyMap();
    }

    private int calculateCalorieTarget(UserProfile profile) {
        if (profile.getWeight() == null || profile.getHeight() == null) {
            return 1500;
        }

        int age = 30;
        if (profile.getBirthDate() != null) {
            age = LocalDate.now().getYear() - profile.getBirthDate().getYear();
        }

        double bmr;
        if (profile.getGender() != null && "MALE".equals(profile.getGender().name())) {
            bmr = 10 * profile.getWeight() + 6.25 * profile.getHeight() - 5 * age + 5;
        } else {
            bmr = 10 * profile.getWeight() + 6.25 * profile.getHeight() - 5 * age - 161;
        }

        double activityMultiplier = profile.getActivityLevel() != null
                ? profile.getActivityLevel().getMultiplier()
                : 1.375;

        double tdee = bmr * activityMultiplier;

        double adjustment = 0.8;
        if (profile.getGoalType() != null) {
            adjustment = switch (profile.getGoalType().name()) {
                case "WEIGHT_LOSS" -> 0.8;
                case "MAINTENANCE" -> 1.0;
                case "MUSCLE_GAIN" -> 1.1;
                default -> {
                    log.warn("[DailyPlan] 알 수 없는 goalType: {}. 기본값 0.8 적용.",
                            profile.getGoalType().name());
                    yield 0.8;
                }
            };
        }

        return (int) Math.round(tdee * adjustment);
    }
}