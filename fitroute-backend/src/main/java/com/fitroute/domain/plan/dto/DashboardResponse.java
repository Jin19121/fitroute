package com.fitroute.domain.plan.dto;

import com.fitroute.domain.plan.entity.PlanItem;
import com.fitroute.global.enums.PlanItemStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
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
    public static class MealItemDto {

        private Long id;
        private String category;

        // 원본
        private String foodName;
        private int calories;
        private int protein;
        private int carbs;
        private int fat;

        // 실제 표시 (수정 시 반영)
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
                    .isModified(item.getStatus() == PlanItemStatus.MODIFIED)
                    .build();
        }
    }

    // ──────────────────────────────────────────
    // WorkoutItemDto
    // ──────────────────────────────────────────
    @Getter
    @Builder
    public static class WorkoutItemDto {

        private Long id;
        private String category;

        // 원본
        private String exerciseName;
        private int calories;
        private Integer sets;
        private Integer reps;

        // 실제 표시 (수정 시 반영)
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
                    .isModified(item.getStatus() == PlanItemStatus.MODIFIED)
                    .build();
        }
    }
}