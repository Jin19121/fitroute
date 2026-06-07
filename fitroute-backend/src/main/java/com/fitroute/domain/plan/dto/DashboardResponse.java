// domain/plan/dto/DashboardResponse.java
package com.fitroute.domain.plan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fitroute.domain.plan.entity.PlanItem;
import com.fitroute.global.enums.PlanItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DashboardResponse {

    private String planStatus;
    private Long planId;
    private String userName;

    // 목표
    private float goalWeight;
    private float currentWeight;
    private float weightToLose;
    private int targetPeriodWeeks;
    private int daysRemaining;
    private LocalDate startDate;
    private int targetCaloriesPerDay;
    private int weeklyAchievementRate;

    // 오늘 데이터
    private TodayData today;

    // ──────────────────────────────────────────
    // TodayData
    // ──────────────────────────────────────────
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TodayData {
        private LocalDate date;
        private int consumedCalories;
        private int burnedCalories;
        private int remainingCalories;
        private List<MealItemDto> meals;
        private List<WorkoutItemDto> workouts;
    }

    // ──────────────────────────────────────────
    // MealItemDto
    // ──────────────────────────────────────────
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MealItemDto {

        private Long id;
        private String category;

        private String foodName;
        private int calories;
        private int protein;
        private int carbs;
        private int fat;

        private String effectiveName;
        private int effectiveCalories;
        private boolean isModified;

        private PlanItemStatus status;

        public static MealItemDto from(PlanItem item) {
            return MealItemDto.builder()
                    .id(item.getId())
                    .category(item.getCategory().name())
                    .foodName(item.getFoodName())
                    .effectiveName(item.getEffectiveName())
                    .calories(item.getCalories())
                    .effectiveCalories(item.getEffectiveCalories())
                    .protein(item.getProtein() != null ? item.getProtein() : 0)
                    .carbs(item.getCarbs() != null ? item.getCarbs() : 0)
                    .fat(item.getFat() != null ? item.getFat() : 0)
                    .status(item.getStatus())
                    .isModified(item.isModified())
                    .build();
        }
    }

    // ──────────────────────────────────────────
    // WorkoutItemDto
    // ──────────────────────────────────────────
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkoutItemDto {

        private Long id;
        private String category;

        private String exerciseName;
        private int calories;
        private Integer sets;
        private Integer reps;

        private String effectiveName;
        private int effectiveCalories;
        private boolean isModified;

        private PlanItemStatus status;

        public static WorkoutItemDto from(PlanItem item) {
            return WorkoutItemDto.builder()
                    .id(item.getId())
                    .category(item.getCategory().name())
                    .exerciseName(item.getExerciseName())
                    .effectiveName(item.getEffectiveName())
                    .calories(item.getCalories())
                    .effectiveCalories(item.getEffectiveCalories())
                    .sets(item.getSets())
                    .reps(item.getReps())
                    .status(item.getStatus())
                    .isModified(item.isModified())
                    .build();
        }
    }
}