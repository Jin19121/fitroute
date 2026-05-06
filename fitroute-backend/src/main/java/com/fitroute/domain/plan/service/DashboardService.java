package com.fitroute.domain.plan.service;

import com.fitroute.domain.plan.dto.DashboardResponse;
import com.fitroute.domain.plan.dto.PlanItemActionRequest;
import com.fitroute.domain.plan.entity.Plan;
import com.fitroute.domain.plan.entity.PlanItem;
import com.fitroute.domain.plan.repository.PlanItemRepository;
import com.fitroute.domain.plan.repository.PlanRepository;
import com.fitroute.domain.user.entity.UserProfile;
import com.fitroute.domain.user.repository.UserProfileRepository;
import com.fitroute.global.enums.PlanItemStatus;
import com.fitroute.global.enums.PlanItemType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final PlanRepository planRepository;
    private final PlanItemRepository planItemRepository;
    private final UserProfileRepository userProfileRepository;

    public DashboardResponse getDashboard(Long userId) {
        // 1. 최신 플랜 조회
        Plan plan = planRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElse(null);

        if (plan == null) {
            return DashboardResponse.builder()
                    .planStatus("NO_PLAN")
                    .build();
        }

        // GENERATING / FAILED 상태는 최소 정보만 반환
        if (!"ACTIVE".equals(plan.getStatus().name())) {
            return DashboardResponse.builder()
                    .planStatus(plan.getStatus().name())
                    .planId(plan.getId())
                    .build();
        }

        // 2. 프로필 조회
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("UserProfile not found: userId=" + userId));

        // 3. 오늘 아이템 조회
        LocalDate today = LocalDate.now();
        List<PlanItem> todayItems = planItemRepository.findByPlanIdAndDate(plan.getId(), today);

        List<PlanItem> meals = todayItems.stream()
                .filter(i -> i.getType() == PlanItemType.MEAL)
                .collect(Collectors.toList());

        List<PlanItem> workouts = todayItems.stream()
                .filter(i -> i.getType() == PlanItemType.WORKOUT)
                .collect(Collectors.toList());

        // 4. 칼로리 계산 (완료 + 수정 항목 집계)
        int consumedCalories = meals.stream()
                .filter(i -> i.getStatus() == PlanItemStatus.COMPLETED
                        || i.getStatus() == PlanItemStatus.MODIFIED)
                .mapToInt(PlanItem::getEffectiveCalories)
                .sum();

        int burnedCalories = workouts.stream()
                .filter(i -> i.getStatus() == PlanItemStatus.COMPLETED
                        || i.getStatus() == PlanItemStatus.MODIFIED)
                .mapToInt(PlanItem::getEffectiveCalories)
                .sum();

        int targetCalories = plan.getTotalCalories() != null ? plan.getTotalCalories() : 0;
        int remainingCalories = Math.max(0, targetCalories - consumedCalories);

        // 5. 주간 달성률 계산
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        long completedThisWeek = planItemRepository.countCompletedByPlanIdAndDateBetween(
                plan.getId(), weekStart, today);
        long activeThisWeek = planItemRepository.countActiveByPlanIdAndDateBetween(
                plan.getId(), weekStart, today);
        int weeklyRate = activeThisWeek > 0
                ? (int) (completedThisWeek * 100 / activeThisWeek)
                : 0;

        // 6. D-Day 계산
        long daysRemaining = plan.getStartDate() != null
                ? Math.max(0, ChronoUnit.DAYS.between(
                        today,
                        plan.getStartDate().plusWeeks(profile.getTargetPeriod())))
                : 0;

        float weightToLose = Math.max(0, profile.getWeight() - profile.getTargetWeight());

        return DashboardResponse.builder()
                .planStatus(plan.getStatus().name())
                .planId(plan.getId())
                .userName(extractUserName(profile))
                .goalWeight(profile.getTargetWeight())
                .currentWeight(profile.getWeight())
                .weightToLose(weightToLose)
                .targetPeriodWeeks(profile.getTargetPeriod())
                .daysRemaining((int) daysRemaining)
                .startDate(plan.getStartDate())
                .targetCaloriesPerDay(targetCalories)
                .weeklyAchievementRate(weeklyRate)
                .today(DashboardResponse.TodayData.builder()
                        .date(today)
                        .consumedCalories(consumedCalories)
                        .burnedCalories(burnedCalories)
                        .remainingCalories(remainingCalories)
                        .meals(meals.stream()
                                .map(DashboardResponse.MealItemDto::from)
                                .collect(Collectors.toList()))
                        .workouts(workouts.stream()
                                .map(DashboardResponse.WorkoutItemDto::from)
                                .collect(Collectors.toList()))
                        .build())
                .build();
    }


    @Transactional
    public void applyItemAction(Long itemId, Long userId, PlanItemActionRequest req) {
            req.validateModifiedFields();

            PlanItem item = planItemRepository.findById(itemId)
                            .orElseThrow(() -> new IllegalArgumentException("PlanItem not found: id=" + itemId));

            if (!item.getPlan().getUserId().equals(userId)) {
                    throw new SecurityException("Access denied to planItem: id=" + itemId);
            }

            switch (req.getStatus()) {
                    case COMPLETED -> item.complete();
                    case SKIPPED -> item.skip();
                    case MODIFIED -> item.modify(
                                    req.getModifiedName(),
                                    req.getModifiedCalories(),
                                    req.getModifiedProtein(),
                                    req.getModifiedCarbs(),
                                    req.getModifiedFat(),
                                    req.getModifiedSets(),
                                    req.getModifiedReps());
                    case PENDING -> item.resetToPending();
                    default -> throw new IllegalArgumentException("지원하지 않는 status: " + req.getStatus());
            }
    }

    private String extractUserName(UserProfile profile) {
        return "사용자";
    }
}