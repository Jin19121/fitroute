// domain/report/dto/DailyReportResponse.java
package com.fitroute.domain.report.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class DailyReportResponse {

    private LocalDate date;
    private String dayOfWeek;

    private WorkoutDetail workout;
    private DietDetail diet;
    private WeightDetail weight;

    // ─── 운동 상세 ──────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class WorkoutDetail {
        private String status;
        private int burnedKcal;
        private List<WorkoutItem> items;
    }

    @Getter
    @Builder
    public static class WorkoutItem {
        private String name;
        private String status;

        /** 원본 칼로리 (PlanItem.calories) */
        private int originalCalories;

        /** 실제 칼로리 (수정 없으면 original 과 동일) */
        private int actualCalories;

        /** actual - original (0이면 수정 없음) */
        private Integer diffCalories;

        private Integer sets;
        private Integer reps;
    }

    // ─── 식단 상세 ──────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class DietDetail {
        private String status;
        private int consumedKcal;
        private int targetKcal;
        private MealBreakdown meals;
    }

    @Getter
    @Builder
    public static class MealBreakdown {
        private MealItem breakfast;
        private MealItem lunch;
        private MealItem dinner;
        private MealItem snack;
    }

    @Getter
    @Builder
    public static class MealItem {
        private String name;
        private String status;

        /** 원본 칼로리 */
        private int originalCalories;

        /** 실제 섭취 칼로리 */
        private int actualCalories;

        /** actual - original */
        private Integer diffCalories;
    }

    // ─── 체중 상세 ──────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class WeightDetail {
        private boolean measured;
        private Float weightKg;
        private String note;
    }
}