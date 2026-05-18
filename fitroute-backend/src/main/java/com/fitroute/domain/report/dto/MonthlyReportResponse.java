// domain/report/dto/MonthlyReportResponse.java
package com.fitroute.domain.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class MonthlyReportResponse {

    private int year;
    private int month;
    private List<DayData> days;
    private Summary summary;

    // ─── 날짜별 데이터 ───────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class DayData {
        private LocalDate date;
        private WorkoutDayData workout;
        private DietDayData diet;
        private WeightDayData weight;
    }

    // 운동: FULL(전체 완수) | PART(일부 완수) | NONE(미실행) | NO_PLAN(계획 없음)
    @Getter
    @Builder
    public static class WorkoutDayData {
        private String status;
        private int burnedKcal;
        private List<String> routineNames;
    }

    // 식단: ACHIEVED(목표 달성) | EXCEEDED(초과) | NO_RECORD(미기록) | NO_PLAN(계획 없음)
    @Getter
    @Builder
    public static class DietDayData {
        private String status;
        private int consumedKcal;
        private int targetKcal;
    }

    // 체중: 측정 여부 + 측정값
    @Getter
    @Builder
    public static class WeightDayData {
        private boolean measured;
        private Float weightKg;
    }

    // ─── KPI 요약 ────────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class Summary {
        private WorkoutSummary workout;
        private DietSummary diet;
        private WeightSummary weight;
    }

    @Getter
    @Builder
    public static class WorkoutSummary {
        private int achievedDays; // FULL 달성 일수
        private int totalPlanDays; // 플랜이 존재한 총 일수
    }

    @Getter
    @Builder
    public static class DietSummary {
        private int achievedDays; // ACHIEVED 달성 일수
        private int totalPlanDays; // 플랜이 존재한 총 일수
        private int avgDailyKcal; // 기록된 날의 일 평균 섭취 kcal
    }

    @Getter
    @Builder
    public static class WeightSummary {
        private Float startWeight; // 월 첫 측정값
        private Float latestWeight; // 월 마지막 측정값
        private Float changeKg; // 변화량 (음수 = 감량)
        private Float goalWeight; // 목표 체중 (UserProfile)
        private long daysToGoal; // 목표 날짜까지 남은 일수
        private List<WeightPoint> measurements; // 차트용 측정 이력
    }

    @Getter
    @Builder
    public static class WeightPoint {
        private LocalDate date;
        private Float weightKg;
    }
}