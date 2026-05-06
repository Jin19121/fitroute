// domain/plan/dto/DailyPlanResponse.java
package com.fitroute.domain.plan.dto;

import com.fitroute.domain.plan.entity.DailyPlan;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Builder
public class DailyPlanResponse {

    private Long planId;
    private LocalDate planDate;
    private Integer calorieTarget;
    private Map<String, Object> mealPlan;
    private Map<String, Object> workoutPlan;

    /**
     * "ACTIVE" | "NO_PLAN"
     * NO_PLAN: 오늘 계획이 없는 경우 (프론트에서 생성 버튼 표시)
     */
    private String status;

    public static DailyPlanResponse from(DailyPlan plan) {
        return DailyPlanResponse.builder()
                .planId(plan.getId())
                .planDate(plan.getPlanDate())
                .calorieTarget(plan.getCalorieTarget())
                .mealPlan(plan.getMealPlan())
                .workoutPlan(plan.getWorkoutPlan())
                .status(plan.getStatus().name())
                .build();
    }
}