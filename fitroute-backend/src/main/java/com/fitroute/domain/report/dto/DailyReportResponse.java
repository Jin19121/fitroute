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
    private String dayOfWeek; // 예: "수요일"

    private WorkoutDetail workout;
    private DietDetail diet;
    private WeightDetail weight;

    // ─── 운동 상세 ──────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class WorkoutDetail {
        private String status; // FULL | PART | NONE | NO_PLAN
        private int burnedKcal;
        private List<WorkoutItem> items;
    }

    @Getter
    @Builder
    public static class WorkoutItem {
        private String name;
        private String status; // COMPLETED | SKIPPED | MODIFIED | PENDING
        private int calories;
        private Integer sets;
        private Integer reps;
    }

    // ─── 식단 상세 ──────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class DietDetail {
        private String status; // ACHIEVED | EXCEEDED | NO_RECORD | NO_PLAN
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
        private int calories;
        private String status; // COMPLETED | SKIPPED | MODIFIED | PENDING
    }

    // ─── 체중 상세 ──────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class WeightDetail {
        private boolean measured;
        private Float weightKg;
        private String note;
    }
}