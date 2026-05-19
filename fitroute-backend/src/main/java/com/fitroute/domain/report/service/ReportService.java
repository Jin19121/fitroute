// domain/report/service/ReportService.java
package com.fitroute.domain.report.service;

import com.fitroute.domain.log.entity.Log;
import com.fitroute.domain.log.entity.LogItem;
import com.fitroute.domain.log.repository.LogItemRepository;
import com.fitroute.domain.log.repository.LogRepository;
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
        private final LogRepository logRepository;
        private final LogItemRepository logItemRepository;
        private final WeightLogRepository weightLogRepository;
        private final UserProfileRepository userProfileRepository;

        private static final double DIET_TOLERANCE = 1.05;

        // ─────────────────────────────────────────────────────────────────────
        // 월간 리포트
        // ─────────────────────────────────────────────────────────────────────

        public MonthlyReportResponse getMonthlyReport(Long userId, int year, int month) {
                YearMonth yearMonth = YearMonth.of(year, month);
                LocalDate from = yearMonth.atDay(1);
                LocalDate to = yearMonth.atEndOfMonth();

                // ── 배치 조회 (N+1 방지) ─────────────────────────────────────────
                List<DailyPlan> plans = dailyPlanRepository
                                .findByUserIdAndPlanDateBetween(userId, from, to);

                Map<LocalDate, DailyPlan> planByDate = plans.stream()
                                .collect(Collectors.toMap(DailyPlan::getPlanDate, p -> p));

                List<Long> planIds = plans.stream().map(DailyPlan::getId).toList();
                Map<Long, List<PlanItem>> itemsByPlanId = new HashMap<>();
                if (!planIds.isEmpty()) {
                        planItemRepository.findByDailyPlanIdIn(planIds)
                                        .forEach(item -> itemsByPlanId
                                                        .computeIfAbsent(item.getDailyPlan().getId(),
                                                                        k -> new ArrayList<>())
                                                        .add(item));
                }

                // ★ PHASE 3: Log 배치 조회
                List<Log> logs = logRepository
                                .findByUserIdAndLogDateBetweenOrderByLogDateAsc(userId, from, to);
                Map<LocalDate, Log> logByDate = logs.stream()
                                .collect(Collectors.toMap(Log::getLogDate, l -> l));

                List<WeightLog> weightLogs = weightLogRepository
                                .findByUserIdAndLogDateBetweenOrderByLogDateAsc(userId, from, to);
                Map<LocalDate, WeightLog> weightByDate = weightLogs.stream()
                                .collect(Collectors.toMap(WeightLog::getLogDate, wl -> wl));

                UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

                // ── 일별 데이터 조립 + KPI 카운터 ──────────────────────────────
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
                        Log dayLog = logByDate.get(date);

                        MonthlyReportResponse.WorkoutDayData workoutDay = buildWorkoutDay(plan, items, dayLog);
                        MonthlyReportResponse.DietDayData dietDay = buildDietDay(plan, items, dayLog);
                        WeightLog wl = weightByDate.get(date);

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

                MonthlyReportResponse.Summary summary = buildSummary(
                                workoutAchievedDays, workoutPlanDays,
                                dietAchievedDays, dietPlanDays, dietTotalKcal, dietRecordedDays,
                                weightLogs, profile);

                return MonthlyReportResponse.builder()
                                .year(year).month(month)
                                .days(days).summary(summary)
                                .build();
        }

        // ─────────────────────────────────────────────────────────────────────
        // 날짜 상세 리포트 (Log 기반 actual vs original 표시)
        // ─────────────────────────────────────────────────────────────────────

        public DailyReportResponse getDayReport(Long userId, LocalDate date) {
                DailyPlan plan = dailyPlanRepository
                                .findByUserIdAndPlanDateAndStatus(userId, date, DailyPlan.PlanStatus.ACTIVE)
                                .orElseGet(() -> dailyPlanRepository
                                                .findTopByUserIdAndPlanDateOrderByVersionDesc(userId, date)
                                                .orElse(null));

                List<PlanItem> items = plan != null
                                ? planItemRepository.findByDailyPlanId(plan.getId())
                                : List.of();

                // LogItem 맵 구성 (plan_item_id → LogItem)
                Map<Long, LogItem> logItemMap = logRepository
                                .findByUserIdAndLogDate(userId, date)
                                .map(log -> logItemRepository.findByLog(log).stream()
                                                .collect(Collectors.toMap(
                                                                li -> li.getPlanItem().getId(), li -> li,
                                                                (a, b) -> a)))
                                .orElse(Map.of());

                WeightLog wl = weightLogRepository.findByUserIdAndLogDate(userId, date).orElse(null);

                return DailyReportResponse.builder()
                                .date(date)
                                .dayOfWeek(toDayOfWeekKr(date))
                                .workout(buildWorkoutDetail(plan, items, logItemMap))
                                .diet(buildDietDetail(plan, items, logItemMap))
                                .weight(DailyReportResponse.WeightDetail.builder()
                                                .measured(wl != null)
                                                .weightKg(wl != null ? wl.getWeightKg() : null)
                                                .note(wl != null ? wl.getNote() : null)
                                                .build())
                                .build();
        }

        // ─────────────────────────────────────────────────────────────────────
        // 운동 — 캘린더용
        // ─────────────────────────────────────────────────────────────────────

        private MonthlyReportResponse.WorkoutDayData buildWorkoutDay(
                        DailyPlan plan, List<PlanItem> items, Log dayLog) {

                if (plan == null)
                        return MonthlyReportResponse.WorkoutDayData.builder()
                                        .status("NO_PLAN").burnedKcal(0).routineNames(List.of()).build();

                List<PlanItem> workouts = items.stream()
                                .filter(i -> i.getType() == PlanItemType.WORKOUT)
                                .filter(i -> i.getCategory() != PlanItemCategory.REST)
                                .toList();

                if (workouts.isEmpty())
                        return MonthlyReportResponse.WorkoutDayData.builder()
                                        .status("NONE").burnedKcal(0).routineNames(List.of()).build();

                List<PlanItem> done = workouts.stream().filter(this::isCompleted).toList();

                String status;
                if (done.isEmpty())
                        status = "NONE";
                else if (done.size() == workouts.size())
                        status = "FULL";
                else
                        status = "PART";

                // ★ Log에 burnedCalories가 집계돼 있으면 그것을 사용
                int burnedKcal = dayLog != null && dayLog.getBurnedCalories() > 0
                                ? dayLog.getBurnedCalories()
                                : done.stream().mapToInt(PlanItem::getEffectiveCalories).sum();

                List<String> names = done.stream().map(PlanItem::getEffectiveName).toList();

                return MonthlyReportResponse.WorkoutDayData.builder()
                                .status(status).burnedKcal(burnedKcal).routineNames(names).build();
        }

        // ─────────────────────────────────────────────────────────────────────
        // 식단 — 캘린더용
        // ─────────────────────────────────────────────────────────────────────

        private MonthlyReportResponse.DietDayData buildDietDay(
                        DailyPlan plan, List<PlanItem> items, Log dayLog) {

                if (plan == null)
                        return MonthlyReportResponse.DietDayData.builder()
                                        .status("NO_PLAN").consumedKcal(0).targetKcal(0).build();

                int target = plan.getCalorieTarget() != null ? plan.getCalorieTarget() : 0;

                // ★ Log 우선 사용
                if (dayLog != null && dayLog.getConsumedCalories() > 0) {
                        int consumed = dayLog.getConsumedCalories();
                        String status = (target > 0 && consumed <= target * DIET_TOLERANCE)
                                        ? "ACHIEVED"
                                        : "EXCEEDED";
                        return MonthlyReportResponse.DietDayData.builder()
                                        .status(status).consumedKcal(consumed).targetKcal(target).build();
                }

                // Fallback: PlanItem 직접 집계
                List<PlanItem> recorded = items.stream()
                                .filter(i -> i.getType() == PlanItemType.MEAL)
                                .filter(this::isCompleted)
                                .toList();

                if (recorded.isEmpty())
                        return MonthlyReportResponse.DietDayData.builder()
                                        .status("NO_RECORD").consumedKcal(0).targetKcal(target).build();

                int consumed = recorded.stream().mapToInt(PlanItem::getEffectiveCalories).sum();
                String status = (target > 0 && consumed <= target * DIET_TOLERANCE)
                                ? "ACHIEVED"
                                : "EXCEEDED";

                return MonthlyReportResponse.DietDayData.builder()
                                .status(status).consumedKcal(consumed).targetKcal(target).build();
        }

        // ─────────────────────────────────────────────────────────────────────
        // 운동 — 날짜 클릭 상세 (actual vs original)
        // ─────────────────────────────────────────────────────────────────────

        private DailyReportResponse.WorkoutDetail buildWorkoutDetail(
                        DailyPlan plan, List<PlanItem> items, Map<Long, LogItem> logItemMap) {

                if (plan == null)
                        return DailyReportResponse.WorkoutDetail.builder()
                                        .status("NO_PLAN").burnedKcal(0).items(List.of()).build();

                List<PlanItem> workouts = items.stream()
                                .filter(i -> i.getType() == PlanItemType.WORKOUT)
                                .filter(i -> i.getCategory() != PlanItemCategory.REST)
                                .toList();

                List<DailyReportResponse.WorkoutItem> workoutItems = workouts.stream()
                                .map(i -> {
                                        LogItem li = logItemMap.get(i.getId());
                                        int origCal = i.getCalories();
                                        int actualCal = li != null && li.getActualCalories() != null
                                                        ? li.getActualCalories()
                                                        : i.getEffectiveCalories();
                                        Integer diff = li != null ? li.getDiffCalories() : 0;

                                        return DailyReportResponse.WorkoutItem.builder()
                                                        .name(i.getEffectiveName())
                                                        .status(i.getStatus().name())
                                                        .originalCalories(origCal)
                                                        .actualCalories(actualCal)
                                                        .diffCalories(diff)
                                                        .sets(i.getSets())
                                                        .reps(i.getReps())
                                                        .build();
                                })
                                .toList();

                long doneCount = workouts.stream().filter(this::isCompleted).count();
                int burnedKcal = workoutItems.stream()
                                .filter(wi -> "COMPLETED".equals(wi.getStatus()))
                                .mapToInt(DailyReportResponse.WorkoutItem::getActualCalories).sum();

                String status = doneCount == 0 ? "NONE"
                                : doneCount == workouts.size() ? "FULL" : "PART";

                return DailyReportResponse.WorkoutDetail.builder()
                                .status(status).burnedKcal(burnedKcal).items(workoutItems).build();
        }

        // ─────────────────────────────────────────────────────────────────────
        // 식단 — 날짜 클릭 상세 (actual vs original)
        // ─────────────────────────────────────────────────────────────────────

        private DailyReportResponse.DietDetail buildDietDetail(
                        DailyPlan plan, List<PlanItem> items, Map<Long, LogItem> logItemMap) {

                if (plan == null)
                        return DailyReportResponse.DietDetail.builder()
                                        .status("NO_PLAN").consumedKcal(0).targetKcal(0).build();

                int target = plan.getCalorieTarget() != null ? plan.getCalorieTarget() : 0;
                List<PlanItem> meals = items.stream().filter(i -> i.getType() == PlanItemType.MEAL).toList();
                List<PlanItem> recorded = meals.stream().filter(this::isCompleted).toList();

                int consumed = recorded.stream().mapToInt(i -> {
                        LogItem li = logItemMap.get(i.getId());
                        return (li != null && li.getActualCalories() != null)
                                        ? li.getActualCalories()
                                        : i.getEffectiveCalories();
                }).sum();

                String status = recorded.isEmpty() ? "NO_RECORD"
                                : (target > 0 && consumed <= target * DIET_TOLERANCE) ? "ACHIEVED" : "EXCEEDED";

                return DailyReportResponse.DietDetail.builder()
                                .status(status).consumedKcal(consumed).targetKcal(target)
                                .meals(DailyReportResponse.MealBreakdown.builder()
                                                .breakfast(findMealItem(meals, PlanItemCategory.BREAKFAST, logItemMap))
                                                .lunch(findMealItem(meals, PlanItemCategory.LUNCH, logItemMap))
                                                .dinner(findMealItem(meals, PlanItemCategory.DINNER, logItemMap))
                                                .snack(findMealItem(meals, PlanItemCategory.SNACK, logItemMap))
                                                .build())
                                .build();
        }

        private DailyReportResponse.MealItem findMealItem(
                        List<PlanItem> meals,
                        PlanItemCategory cat,
                        Map<Long, LogItem> logItemMap) {

                return meals.stream()
                                .filter(i -> i.getCategory() == cat)
                                .findFirst()
                                .map(i -> {
                                        LogItem li = logItemMap.get(i.getId());
                                        int origCal = i.getCalories();
                                        int actualCal = (li != null && li.getActualCalories() != null)
                                                        ? li.getActualCalories()
                                                        : i.getEffectiveCalories();
                                        Integer diffCal = li != null ? li.getDiffCalories() : 0;
                                        return DailyReportResponse.MealItem.builder()
                                                        .name(i.getEffectiveName())
                                                        .status(i.getStatus().name())
                                                        .originalCalories(origCal)
                                                        .actualCalories(actualCal)
                                                        .diffCalories(diffCal)
                                                        .build();
                                })
                                .orElse(null);
        }

        // ─────────────────────────────────────────────────────────────────────
        // KPI 요약 (Log 기반 집계)
        // ─────────────────────────────────────────────────────────────────────

        private MonthlyReportResponse.Summary buildSummary(
                        int workoutAchievedDays, int workoutPlanDays,
                        int dietAchievedDays, int dietPlanDays,
                        int dietTotalKcal, int dietRecordedDays,
                        List<WeightLog> weightLogs, UserProfile profile) {

                MonthlyReportResponse.WorkoutSummary workoutSummary = MonthlyReportResponse.WorkoutSummary.builder()
                                .achievedDays(workoutAchievedDays)
                                .totalPlanDays(workoutPlanDays)
                                .build();

                int avgKcal = dietRecordedDays > 0 ? dietTotalKcal / dietRecordedDays : 0;
                MonthlyReportResponse.DietSummary dietSummary = MonthlyReportResponse.DietSummary.builder()
                                .achievedDays(dietAchievedDays)
                                .totalPlanDays(dietPlanDays)
                                .avgDailyKcal(avgKcal)
                                .build();

                Float startWeight = weightLogs.isEmpty() ? null : weightLogs.get(0).getWeightKg();
                Float latestWeight = weightLogs.isEmpty() ? null
                                : weightLogs.get(weightLogs.size() - 1).getWeightKg();
                Float changeKg = (startWeight != null && latestWeight != null)
                                ? Math.round((latestWeight - startWeight) * 10) / 10.0f
                                : null;

                long daysToGoal = 0;
                if (profile != null && profile.getTargetPeriod() != null && !weightLogs.isEmpty()) {
                        LocalDate endDate = weightLogs.get(0).getLogDate()
                                        .plusWeeks(profile.getTargetPeriod());
                        daysToGoal = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), endDate));
                }

                List<MonthlyReportResponse.WeightPoint> points = weightLogs.stream()
                                .map(wl -> MonthlyReportResponse.WeightPoint.builder()
                                                .date(wl.getLogDate()).weightKg(wl.getWeightKg()).build())
                                .toList();

                MonthlyReportResponse.WeightSummary weightSummary = MonthlyReportResponse.WeightSummary.builder()
                                .startWeight(startWeight).latestWeight(latestWeight).changeKg(changeKg)
                                .goalWeight(profile != null ? profile.getTargetWeight() : null)
                                .daysToGoal(daysToGoal).measurements(points)
                                .build();

                return MonthlyReportResponse.Summary.builder()
                                .workout(workoutSummary).diet(dietSummary).weight(weightSummary)
                                .build();
        }

        // ─────────────────────────────────────────────────────────────────────
        // 공통 헬퍼
        // ─────────────────────────────────────────────────────────────────────

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