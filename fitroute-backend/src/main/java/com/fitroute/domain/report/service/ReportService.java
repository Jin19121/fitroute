// domain/report/service/ReportService.java
package com.fitroute.domain.report.service;

import com.fitroute.domain.plan.entity.DailyPlan;
import com.fitroute.domain.plan.entity.PlanItem;
import com.fitroute.domain.plan.repository.DailyPlanRepository;
import com.fitroute.domain.plan.repository.PlanItemRepository;
import com.fitroute.domain.report.dto.DailyReportResponse;
import com.fitroute.domain.report.dto.MonthlyReportResponse;
import com.fitroute.domain.user.entity.UserProfile;
import com.fitroute.domain.user.repository.UserProfileRepository;
import com.fitroute.domain.weight.entity.WeightLog;
import com.fitroute.domain.weight.repository.WeightLogRepository;
import com.fitroute.global.enums.PlanItemCategory;
import com.fitroute.global.enums.PlanItemStatus;
import com.fitroute.global.enums.PlanItemType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final DailyPlanRepository dailyPlanRepository;
    private final PlanItemRepository planItemRepository;
    private final WeightLogRepository weightLogRepository;
    private final UserProfileRepository userProfileRepository;

    // 목표 kcal 5% 초과까지 ACHIEVED로 허용
    private static final double DIET_TOLERANCE = 1.05;

    // ─── 월간 리포트 ─────────────────────────────────────────────────────────

    public MonthlyReportResponse getMonthlyReport(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate from = yearMonth.atDay(1);
        LocalDate to = yearMonth.atEndOfMonth();

        // 1. 배치 조회 — N+1 방지
        List<DailyPlan> plans = dailyPlanRepository
                .findByUserIdAndPlanDateBetween(userId, from, to);

        Map<LocalDate, DailyPlan> planByDate = plans.stream()
                .collect(Collectors.toMap(DailyPlan::getPlanDate, p -> p));

        // DailyPlan ID 목록으로 해당 월의 모든 PlanItem 한 번에 조회
        List<Long> planIds = plans.stream().map(DailyPlan::getId).toList();
        Map<Long, List<PlanItem>> itemsByPlanId = new HashMap<>();
        if (!planIds.isEmpty()) {
            planItemRepository.findByDailyPlanIdIn(planIds)
                    .forEach(item -> itemsByPlanId
                            .computeIfAbsent(item.getDailyPlan().getId(), k -> new ArrayList<>())
                            .add(item));
        }

        // ★ 수정: logDate 컬럼명 사용 (WeightLog 엔티티와 일치)
        List<WeightLog> weightLogs = weightLogRepository
                        .findByUserIdAndLogDateBetweenOrderByLogDateAsc(userId, from, to);
        Map<LocalDate, WeightLog> weightByDate = weightLogs.stream()
                        .collect(Collectors.toMap(WeightLog::getLogDate, wl -> wl));

        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        // 2. 일별 데이터 조립 + KPI 카운터 누적
        List<MonthlyReportResponse.DayData> days = new ArrayList<>();

        int workoutAchievedDays = 0, workoutPlanDays = 0;
        int dietAchievedDays = 0, dietPlanDays = 0;
        int dietTotalKcal = 0, dietRecordedDays = 0;

        for (int d = 1; d <= from.lengthOfMonth(); d++) {
            LocalDate date = LocalDate.of(year, month, d);
            DailyPlan plan = planByDate.get(date);
            List<PlanItem> items = plan != null
                    ? itemsByPlanId.getOrDefault(plan.getId(), List.of())
                    : List.of();

            MonthlyReportResponse.WorkoutDayData workoutDay = buildWorkoutDay(plan, items);
            MonthlyReportResponse.DietDayData dietDay = buildDietDay(plan, items);
            WeightLog wl = weightByDate.get(date);

            // KPI 집계
            if (plan != null) {
                workoutPlanDays++;
                if ("FULL".equals(workoutDay.getStatus()))
                    workoutAchievedDays++;

                dietPlanDays++;
                if ("ACHIEVED".equals(dietDay.getStatus()))
                    dietAchievedDays++;
                if (!"NO_RECORD".equals(dietDay.getStatus())) {
                    dietTotalKcal += dietDay.getConsumedKcal();
                    dietRecordedDays++;
                }
            }

            days.add(MonthlyReportResponse.DayData.builder()
                    .date(date)
                    .workout(workoutDay)
                    .diet(dietDay)
                    .weight(MonthlyReportResponse.WeightDayData.builder()
                            .measured(wl != null)
                            .weightKg(wl != null ? wl.getWeightKg() : null)
                            .build())
                    .build());
        }

        // 3. Summary 조립
        MonthlyReportResponse.Summary summary = buildSummary(
                workoutAchievedDays, workoutPlanDays,
                dietAchievedDays, dietPlanDays, dietTotalKcal, dietRecordedDays,
                weightLogs, profile);

        return MonthlyReportResponse.builder()
                .year(year).month(month)
                .days(days).summary(summary)
                .build();
    }

    // ─── 날짜 상세 ───────────────────────────────────────────────────────────

    public DailyReportResponse getDayReport(Long userId, LocalDate date) {
        DailyPlan plan = dailyPlanRepository
                .findByUserIdAndPlanDate(userId, date).orElse(null);

        List<PlanItem> items = plan != null
                ? planItemRepository.findByDailyPlanId(plan.getId())
                : List.of();

        // ★ 수정: logDate 컬럼명 사용
        WeightLog wl = weightLogRepository
                        .findByUserIdAndLogDate(userId, date).orElse(null);

        return DailyReportResponse.builder()
                .date(date)
                .dayOfWeek(toDayOfWeekKr(date))
                .workout(buildWorkoutDetail(plan, items))
                .diet(buildDietDetail(plan, items))
                .weight(DailyReportResponse.WeightDetail.builder()
                        .measured(wl != null)
                        .weightKg(wl != null ? wl.getWeightKg() : null)
                        .note(wl != null ? wl.getNote() : null)
                        .build())
                .build();
    }

    // ─── 운동 — 캘린더용 상태 판정 ───────────────────────────────────────────

    private MonthlyReportResponse.WorkoutDayData buildWorkoutDay(
            DailyPlan plan, List<PlanItem> items) {

        if (plan == null) {
            return MonthlyReportResponse.WorkoutDayData.builder()
                    .status("NO_PLAN").burnedKcal(0).routineNames(List.of()).build();
        }

        List<PlanItem> workouts = items.stream()
                .filter(i -> i.getType() == PlanItemType.WORKOUT)
                .filter(i -> i.getCategory() != PlanItemCategory.REST)
                .toList();

        if (workouts.isEmpty()) {
            return MonthlyReportResponse.WorkoutDayData.builder()
                    .status("NONE").burnedKcal(0).routineNames(List.of()).build();
        }

        List<PlanItem> done = workouts.stream().filter(this::isCompleted).toList();

        String status;
        if (done.isEmpty())
            status = "NONE";
        else if (done.size() == workouts.size())
            status = "FULL";
        else
            status = "PART";

        int burnedKcal = done.stream().mapToInt(PlanItem::getEffectiveCalories).sum();
        List<String> names = done.stream().map(PlanItem::getEffectiveName).toList();

        return MonthlyReportResponse.WorkoutDayData.builder()
                .status(status).burnedKcal(burnedKcal).routineNames(names).build();
    }

    // ─── 식단 — 캘린더용 상태 판정 ───────────────────────────────────────────

    private MonthlyReportResponse.DietDayData buildDietDay(
            DailyPlan plan, List<PlanItem> items) {

        if (plan == null) {
            return MonthlyReportResponse.DietDayData.builder()
                    .status("NO_PLAN").consumedKcal(0).targetKcal(0).build();
        }

        int target = plan.getCalorieTarget() != null ? plan.getCalorieTarget() : 0;

        List<PlanItem> recorded = items.stream()
                .filter(i -> i.getType() == PlanItemType.MEAL)
                .filter(this::isCompleted)
                .toList();

        if (recorded.isEmpty()) {
            return MonthlyReportResponse.DietDayData.builder()
                    .status("NO_RECORD").consumedKcal(0).targetKcal(target).build();
        }

        int consumed = recorded.stream().mapToInt(PlanItem::getEffectiveCalories).sum();
        String status = (target > 0 && consumed <= target * DIET_TOLERANCE)
                ? "ACHIEVED"
                : "EXCEEDED";

        return MonthlyReportResponse.DietDayData.builder()
                .status(status).consumedKcal(consumed).targetKcal(target).build();
    }

    // ─── 운동 — 날짜 클릭 상세 ───────────────────────────────────────────────

    private DailyReportResponse.WorkoutDetail buildWorkoutDetail(
            DailyPlan plan, List<PlanItem> items) {

        if (plan == null) {
            return DailyReportResponse.WorkoutDetail.builder()
                    .status("NO_PLAN").burnedKcal(0).items(List.of()).build();
        }

        List<PlanItem> workouts = items.stream()
                .filter(i -> i.getType() == PlanItemType.WORKOUT)
                .filter(i -> i.getCategory() != PlanItemCategory.REST)
                .toList();

        List<DailyReportResponse.WorkoutItem> workoutItems = workouts.stream()
                .map(i -> DailyReportResponse.WorkoutItem.builder()
                        .name(i.getEffectiveName())
                        .status(i.getStatus().name())
                        .calories(i.getEffectiveCalories())
                        .sets(i.getSets())
                        .reps(i.getReps())
                        .build())
                .toList();

        long doneCount = workouts.stream().filter(this::isCompleted).count();
        int burnedKcal = workouts.stream().filter(this::isCompleted)
                .mapToInt(PlanItem::getEffectiveCalories).sum();

        String status;
        if (doneCount == 0)
            status = "NONE";
        else if (doneCount == workouts.size())
            status = "FULL";
        else
            status = "PART";

        return DailyReportResponse.WorkoutDetail.builder()
                .status(status).burnedKcal(burnedKcal).items(workoutItems).build();
    }

    // ─── 식단 — 날짜 클릭 상세 ───────────────────────────────────────────────

    private DailyReportResponse.DietDetail buildDietDetail(
            DailyPlan plan, List<PlanItem> items) {

        if (plan == null) {
            return DailyReportResponse.DietDetail.builder()
                    .status("NO_PLAN").consumedKcal(0).targetKcal(0).build();
        }

        int target = plan.getCalorieTarget() != null ? plan.getCalorieTarget() : 0;
        List<PlanItem> meals = items.stream().filter(i -> i.getType() == PlanItemType.MEAL).toList();
        List<PlanItem> recorded = meals.stream().filter(this::isCompleted).toList();
        int consumed = recorded.stream().mapToInt(PlanItem::getEffectiveCalories).sum();

        String status = recorded.isEmpty() ? "NO_RECORD"
                : (target > 0 && consumed <= target * DIET_TOLERANCE) ? "ACHIEVED" : "EXCEEDED";

        return DailyReportResponse.DietDetail.builder()
                .status(status).consumedKcal(consumed).targetKcal(target)
                .meals(DailyReportResponse.MealBreakdown.builder()
                        .breakfast(findMeal(meals, PlanItemCategory.BREAKFAST))
                        .lunch(findMeal(meals, PlanItemCategory.LUNCH))
                        .dinner(findMeal(meals, PlanItemCategory.DINNER))
                        .snack(findMeal(meals, PlanItemCategory.SNACK))
                        .build())
                .build();
    }

    private DailyReportResponse.MealItem findMeal(List<PlanItem> meals, PlanItemCategory cat) {
        return meals.stream()
                .filter(i -> i.getCategory() == cat)
                .findFirst()
                .map(i -> DailyReportResponse.MealItem.builder()
                        .name(i.getEffectiveName())
                        .calories(i.getEffectiveCalories())
                        .status(i.getStatus().name())
                        .build())
                .orElse(null);
    }

    // ─── KPI 요약 통계 ───────────────────────────────────────────────────────

    private MonthlyReportResponse.Summary buildSummary(
            int workoutAchievedDays, int workoutPlanDays,
            int dietAchievedDays, int dietPlanDays,
            int dietTotalKcal, int dietRecordedDays,
            List<WeightLog> weightLogs, UserProfile profile) {

        // 운동 요약
        MonthlyReportResponse.WorkoutSummary workoutSummary = MonthlyReportResponse.WorkoutSummary.builder()
                .achievedDays(workoutAchievedDays)
                .totalPlanDays(workoutPlanDays)
                .build();

        // 식단 요약
        int avgKcal = dietRecordedDays > 0 ? dietTotalKcal / dietRecordedDays : 0;
        MonthlyReportResponse.DietSummary dietSummary = MonthlyReportResponse.DietSummary.builder()
                .achievedDays(dietAchievedDays)
                .totalPlanDays(dietPlanDays)
                .avgDailyKcal(avgKcal)
                .build();

        // 체중 요약
        Float startWeight = weightLogs.isEmpty() ? null : weightLogs.get(0).getWeightKg();
        Float latestWeight = weightLogs.isEmpty() ? null
                : weightLogs.get(weightLogs.size() - 1).getWeightKg();
        Float changeKg = (startWeight != null && latestWeight != null)
                ? Math.round((latestWeight - startWeight) * 10) / 10.0f
                : null;

        Float goalWeight = profile != null ? profile.getTargetWeight() : null;

        // D-Day: 첫 측정일 + 목표 기간 - 오늘
        long daysToGoal = 0;
        if (profile != null && profile.getTargetPeriod() != null && !weightLogs.isEmpty()) {
                // ★ 수정: logDate 사용
                LocalDate endDate = weightLogs.get(0).getLogDate()
                    .plusWeeks(profile.getTargetPeriod());
            daysToGoal = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), endDate));
        }

        List<MonthlyReportResponse.WeightPoint> points = weightLogs.stream()
                .map(wl -> MonthlyReportResponse.WeightPoint.builder()
                                        .date(wl.getLogDate()) // ★ 수정: logDate 사용
                        .weightKg(wl.getWeightKg())
                        .build())
                .toList();

        MonthlyReportResponse.WeightSummary weightSummary = MonthlyReportResponse.WeightSummary.builder()
                .startWeight(startWeight)
                .latestWeight(latestWeight)
                .changeKg(changeKg)
                .goalWeight(goalWeight)
                .daysToGoal(daysToGoal)
                .measurements(points)
                .build();

        return MonthlyReportResponse.Summary.builder()
                .workout(workoutSummary)
                .diet(dietSummary)
                .weight(weightSummary)
                .build();
    }

    // ─── 공통 헬퍼 ───────────────────────────────────────────────────────────

    private boolean isCompleted(PlanItem item) {
            return item.getStatus() == PlanItemStatus.COMPLETED;
    }

    private String toDayOfWeekKr(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "월요일";
            case TUESDAY -> "화요일";
            case WEDNESDAY -> "수요일";
            case THURSDAY -> "목요일";
            case FRIDAY -> "금요일";
            case SATURDAY -> "토요일";
            case SUNDAY -> "일요일";
        };
    }
}