// domain/plan/service/DashboardService.java
package com.fitroute.domain.plan.service;

import com.fitroute.domain.log.entity.Log;
import com.fitroute.domain.log.repository.LogRepository;
import com.fitroute.domain.log.service.LogService;
import com.fitroute.domain.plan.dto.DashboardResponse;
import com.fitroute.domain.plan.dto.PlanItemActionRequest;
import com.fitroute.domain.plan.entity.DailyPlan;
import com.fitroute.domain.plan.entity.PlanItem;
import com.fitroute.domain.plan.repository.DailyPlanRepository;
import com.fitroute.domain.plan.repository.PlanItemRepository;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

        private final DailyPlanRepository dailyPlanRepository;
        private final PlanItemRepository planItemRepository;
        private final UserProfileRepository userProfileRepository;
        private final LogRepository logRepository;
        private final LogService logService;

        // ─────────────────────────────────────────────────────────────────────
        // 대시보드 조회
        // ─────────────────────────────────────────────────────────────────────

        public DashboardResponse getDashboard(Long userId) {
                LocalDate today = LocalDate.now();

                // 1. ACTIVE 플랜 조회
                DailyPlan plan = dailyPlanRepository
                                .findByUserIdAndPlanDateAndStatus(userId, today, DailyPlan.PlanStatus.ACTIVE)
                                .orElse(null);

                if (plan == null) {
                        // ACTIVE 없음 → 최신 버전으로 상태만 반환
                        return dailyPlanRepository
                                        .findTopByUserIdAndPlanDateOrderByVersionDesc(userId, today)
                                        .map(p -> DashboardResponse.builder()
                                                        .planStatus(p.getStatus().name())
                                                        .planId(p.getId())
                                                        .build())
                                        .orElseGet(() -> DashboardResponse.builder().planStatus("NO_PLAN").build());
                }

                // 2. 프로필
                UserProfile profile = userProfileRepository.findByUserId(userId)
                                .orElseThrow(() -> new IllegalStateException(
                                                "UserProfile not found: userId=" + userId));

                // 3. 오늘 PlanItem 조회 (상세 목록 표시용)
                List<PlanItem> todayItems = planItemRepository.findByPlanIdAndDate(plan.getId(), today);

                List<PlanItem> meals = todayItems.stream()
                                .filter(i -> i.getType() == PlanItemType.MEAL)
                                .collect(Collectors.toList());
                List<PlanItem> workouts = todayItems.stream()
                                .filter(i -> i.getType() == PlanItemType.WORKOUT)
                                .collect(Collectors.toList());

                // 4. ★ 칼로리 집계: Log 우선 → PlanItem 폴백
                int consumedCalories;
                int burnedCalories;

                Optional<Log> todayLog = logRepository.findByUserIdAndLogDate(userId, today);
                if (todayLog.isPresent()) {
                        // Log 기반 집계 (정확한 값)
                        consumedCalories = todayLog.get().getConsumedCalories();
                        burnedCalories = todayLog.get().getBurnedCalories();
                        log.debug("[Dashboard] Using Log aggregation - consumed={}, burned={}",
                                        consumedCalories, burnedCalories);
                } else {
                        // Log 생성 전 최초 접속 케이스 → PlanItem 직접 집계
                        consumedCalories = meals.stream()
                                        .filter(i -> i.getStatus() == PlanItemStatus.COMPLETED)
                                        .mapToInt(PlanItem::getEffectiveCalories)
                                        .sum();
                        burnedCalories = workouts.stream()
                                        .filter(i -> i.getStatus() == PlanItemStatus.COMPLETED)
                                        .mapToInt(PlanItem::getEffectiveCalories)
                                        .sum();
                        log.debug("[Dashboard] Using PlanItem fallback - consumed={}, burned={}",
                                        consumedCalories, burnedCalories);
                }

                int targetCalories = plan.getCalorieTarget() != null ? plan.getCalorieTarget() : 0;
                int remainingCalories = Math.max(0, targetCalories - consumedCalories);

                // 5. 주간 달성률
                LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
                long completedThisWeek = planItemRepository
                                .countCompletedByPlanIdAndDateBetween(plan.getId(), weekStart, today);
                long activeThisWeek = planItemRepository
                                .countActiveByPlanIdAndDateBetween(plan.getId(), weekStart, today);
                int weeklyRate = activeThisWeek > 0
                                ? (int) (completedThisWeek * 100 / activeThisWeek)
                                : 0;

                // 6. D-Day
                long daysRemaining = 0;
                if (profile.getTargetPeriod() != null) {
                        LocalDate endDate = plan.getPlanDate().plusWeeks(profile.getTargetPeriod());
                        daysRemaining = Math.max(0, ChronoUnit.DAYS.between(today, endDate));
                }

                float weightToLose = (profile.getWeight() != null && profile.getTargetWeight() != null)
                                ? Math.max(0, profile.getWeight() - profile.getTargetWeight())
                                : 0f;

                return DashboardResponse.builder()
                                .planStatus(plan.getStatus().name())
                                .planId(plan.getId())
                                .userName(extractUserName(profile))
                                .goalWeight(profile.getTargetWeight())
                                .currentWeight(profile.getWeight())
                                .weightToLose(weightToLose)
                                .targetPeriodWeeks(profile.getTargetPeriod())
                                .daysRemaining((int) daysRemaining)
                                .startDate(plan.getPlanDate())
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

        // ─────────────────────────────────────────────────────────────────────
        // PlanItem 액션 적용
        // ─────────────────────────────────────────────────────────────────────

        /**
         * PlanItem 상태 변경 + Log 동기화를 단일 트랜잭션으로 처리.
         */
        @Transactional
        public void applyItemAction(Long itemId, Long userId, PlanItemActionRequest req) {
                req.validateModifiedFields();

                PlanItem item = planItemRepository.findById(itemId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "PlanItem not found: id=" + itemId));

                if (!item.getDailyPlan().getUserId().equals(userId)) {
                        throw new SecurityException("Access denied to planItem: id=" + itemId);
                }

                // 비즈니스 로직: action 기반 상태 전환
                switch (req.getAction()) {
                        case COMPLETE -> item.complete();

                        case SKIP -> item.skip();

                        case MODIFY -> item.modify(
                                        req.getModifiedName(),
                                        req.getModifiedCalories(),
                                        req.getModifiedProtein(),
                                        req.getModifiedCarbs(),
                                        req.getModifiedFat(),
                                        req.getModifiedSets(),
                                        req.getModifiedReps());

                        case COMPLETE_WITH_MODIFY -> {
                                item.modify(
                                                req.getModifiedName(),
                                                req.getModifiedCalories(),
                                                req.getModifiedProtein(),
                                                req.getModifiedCarbs(),
                                                req.getModifiedFat(),
                                                req.getModifiedSets(),
                                                req.getModifiedReps());
                                item.complete();
                        }

                        case RESET -> item.resetToPending();

                        default -> throw new IllegalArgumentException(
                                        "지원하지 않는 action: " + req.getAction());
                }

                // ★ PHASE 3: PlanItem 변경 후 Log에 반영 (같은 트랜잭션)
                logService.upsertFromPlanItem(item);
        }

        private String extractUserName(UserProfile profile) {
                return "사용자";
        }
}