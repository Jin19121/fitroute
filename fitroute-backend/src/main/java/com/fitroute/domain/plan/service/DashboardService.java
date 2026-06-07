// domain/plan/service/DashboardService.java
package com.fitroute.domain.plan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.fitroute.global.cache.CacheKeyConstants;
import com.fitroute.global.enums.PlanItemStatus;
import com.fitroute.global.enums.PlanItemType;
import com.fitroute.global.event.DashboardCacheEvictEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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

        // ─── Redis Cache-Aside ────────────────────────────────────────────────
        private final RedisTemplate<String, String> redisTemplate;
        private final ObjectMapper objectMapper;
        private final ApplicationEventPublisher eventPublisher;

        private static final long CACHE_TTL_MINUTES = 15;

        // ─────────────────────────────────────────────────────────────────────
        // 대시보드 조회 — Cache-Aside 패턴
        // ─────────────────────────────────────────────────────────────────────

        public DashboardResponse getDashboard(Long userId) {
                String cacheKey = CacheKeyConstants.TODAY_CACHE_PREFIX + userId;

                // 1. Redis 캐시 조회
                try {
                        String cachedJson = redisTemplate.opsForValue().get(cacheKey);
                        if (cachedJson != null) {
                                log.info("[DashboardCache] Cache Hit - key={}", cacheKey);
                                return objectMapper.readValue(cachedJson, DashboardResponse.class);
                        }
                } catch (Exception e) {
                        // Redis 장애 시 DB Fallback — 서비스 중단 방지
                        log.error("[DashboardCache] Redis 조회 실패, DB Fallback 진행 - error={}", e.getMessage());
                }

                // 2. Cache Miss → DB 조회
                log.info("[DashboardCache] Cache Miss - key={}, DB 조회 시작", cacheKey);
                DashboardResponse response = fetchFromDb(userId);

                // 3. 조회 결과 Redis 캐싱 (TTL 15분)
                try {
                        String json = objectMapper.writeValueAsString(response);
                        redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                        log.info("[DashboardCache] 캐시 저장 완료 - key={}", cacheKey);
                } catch (Exception e) {
                        // Redis 저장 실패해도 응답은 정상 반환
                        log.error("[DashboardCache] Redis 저장 실패 - error={}", e.getMessage());
                }

                return response;
        }

        // ─────────────────────────────────────────────────────────────────────
        // PlanItem 액션 적용 + 캐시 무효화 이벤트 발행
        // ─────────────────────────────────────────────────────────────────────

        @Transactional
        public void applyItemAction(Long itemId, Long userId, PlanItemActionRequest req) {
                req.validateModifiedFields();

                PlanItem item = planItemRepository.findById(itemId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "PlanItem not found: id=" + itemId));

                if (!item.getDailyPlan().getUserId().equals(userId)) {
                        throw new SecurityException("Access denied to planItem: id=" + itemId);
                }

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

                logService.upsertFromPlanItem(item);

                // DB 커밋 완료 후 캐시 무효화 (Race Condition 방지)
                eventPublisher.publishEvent(new DashboardCacheEvictEvent(userId));
        }

        // ─────────────────────────────────────────────────────────────────────
        // DB 조회 (Cache Miss 시 호출)
        // ─────────────────────────────────────────────────────────────────────

        private DashboardResponse fetchFromDb(Long userId) {
                LocalDate today = LocalDate.now();

                DailyPlan plan = dailyPlanRepository
                                .findByUserIdAndPlanDateAndStatus(userId, today, DailyPlan.PlanStatus.ACTIVE)
                                .orElse(null);

                if (plan == null) {
                        return dailyPlanRepository
                                        .findTopByUserIdAndPlanDateOrderByVersionDesc(userId, today)
                                        .map(p -> DashboardResponse.builder()
                                                        .planStatus(p.getStatus().name())
                                                        .planId(p.getId())
                                                        .build())
                                        .orElseGet(() -> DashboardResponse.builder().planStatus("NO_PLAN").build());
                }

                UserProfile profile = userProfileRepository.findByUserId(userId)
                                .orElseThrow(() -> new IllegalStateException(
                                                "UserProfile not found: userId=" + userId));

                List<PlanItem> todayItems = planItemRepository.findByPlanIdAndDate(plan.getId(), today);

                List<PlanItem> meals = todayItems.stream()
                                .filter(i -> i.getType() == PlanItemType.MEAL)
                                .collect(Collectors.toList());
                List<PlanItem> workouts = todayItems.stream()
                                .filter(i -> i.getType() == PlanItemType.WORKOUT)
                                .collect(Collectors.toList());

                int consumedCalories;
                int burnedCalories;

                Optional<Log> todayLog = logRepository.findByUserIdAndLogDate(userId, today);
                if (todayLog.isPresent()) {
                        consumedCalories = todayLog.get().getConsumedCalories();
                        burnedCalories = todayLog.get().getBurnedCalories();
                        log.debug("[Dashboard] Log 집계 사용 - consumed={}, burned={}", consumedCalories, burnedCalories);
                } else {
                        consumedCalories = meals.stream()
                                        .filter(i -> i.getStatus() == PlanItemStatus.COMPLETED)
                                        .mapToInt(PlanItem::getEffectiveCalories)
                                        .sum();
                        burnedCalories = workouts.stream()
                                        .filter(i -> i.getStatus() == PlanItemStatus.COMPLETED)
                                        .mapToInt(PlanItem::getEffectiveCalories)
                                        .sum();
                        log.debug("[Dashboard] PlanItem Fallback 집계 - consumed={}, burned={}", consumedCalories,
                                        burnedCalories);
                }

                int targetCalories = plan.getCalorieTarget() != null ? plan.getCalorieTarget() : 0;
                int remainingCalories = Math.max(0, targetCalories - consumedCalories);

                LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
                long completedThisWeek = planItemRepository
                                .countCompletedByPlanIdAndDateBetween(plan.getId(), weekStart, today);
                long activeThisWeek = planItemRepository
                                .countActiveByPlanIdAndDateBetween(plan.getId(), weekStart, today);
                int weeklyRate = activeThisWeek > 0
                                ? (int) (completedThisWeek * 100 / activeThisWeek)
                                : 0;

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

        private String extractUserName(UserProfile profile) {
                return "사용자";
        }
}