package com.fitroute.domain.log.service;

import com.fitroute.domain.log.entity.Log;
import com.fitroute.domain.log.entity.LogItem;
import com.fitroute.domain.log.repository.LogItemRepository;
import com.fitroute.domain.log.repository.LogRepository;
import com.fitroute.domain.plan.entity.DailyPlan;
import com.fitroute.domain.plan.entity.PlanItem;
import com.fitroute.domain.plan.repository.PlanItemRepository;
import com.fitroute.global.enums.PlanItemStatus;
import com.fitroute.global.enums.PlanItemType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;
    private final LogItemRepository logItemRepository;
    private final PlanItemRepository planItemRepository;

    /**
     * DailyPlan 최초 생성 직후 호출
     * 같은 날짜 Log가 없으면 생성
     */
    @Transactional
    public Log createForPlan(DailyPlan dailyPlan) {

        return logRepository
                .findByUserIdAndLogDate(
                        dailyPlan.getUserId(),
                        dailyPlan.getPlanDate()
                )
                .orElseGet(() -> {

                    Log logEntity = Log.create(
                            dailyPlan.getUserId(),
                            dailyPlan,
                            dailyPlan.getPlanDate()
                    );

                    Log savedLog = logRepository.save(logEntity);

                    log.info(
                            "[LogService] Created log - userId={}, date={}, planId={}",
                            dailyPlan.getUserId(),
                            dailyPlan.getPlanDate(),
                            dailyPlan.getId()
                    );

                    return savedLog;
                });
    }

    /**
     * 플랜 재생성 시 Log 초기화
     */
    @Transactional
    public Log resetForNewPlan(DailyPlan newPlan) {

        return logRepository
                .findByUserIdAndLogDate(
                        newPlan.getUserId(),
                        newPlan.getPlanDate()
                )
                .map(existing -> {

                    logItemRepository.deleteByLog(existing);

                    existing.resetForNewPlan(newPlan);

                    log.info(
                            "[LogService] Reset log for new plan v{} - userId={}, date={}",
                            newPlan.getVersion(),
                            newPlan.getUserId(),
                            newPlan.getPlanDate()
                    );

                    return existing;
                })
                .orElseGet(() -> {

                    Log logEntity = Log.create(
                            newPlan.getUserId(),
                            newPlan,
                            newPlan.getPlanDate()
                    );

                    return logRepository.save(logEntity);
                });
    }

    /**
     * PlanItem 상태 변경 → LogItem 반영
     */
    @Transactional
    public void upsertFromPlanItem(PlanItem item) {

        DailyPlan dailyPlan = item.getDailyPlan();
        Long userId = dailyPlan.getUserId();

        Log logEntity = logRepository
                .findByUserIdAndLogDate(
                        userId,
                        dailyPlan.getPlanDate()
                )
                .orElseGet(() ->
                        logRepository.save(
                                Log.create(
                                        userId,
                                        dailyPlan,
                                        dailyPlan.getPlanDate()
                                )
                        )
                );

        if (item.getStatus() == PlanItemStatus.PENDING) {

            logItemRepository
                    .findByLogAndPlanItem(logEntity, item)
                    .ifPresent(logItemRepository::delete);

        } else {

            LogItem logItem = logItemRepository
                    .findByLogAndPlanItem(logEntity, item)
                    .orElse(
                            LogItem.builder()
                                    .log(logEntity)
                                    .planItem(item)
                                    .build()
                    );

            String originalName =
                    item.getType() == PlanItemType.MEAL
                            ? item.getFoodName()
                            : item.getExerciseName();

            int actualCalories = item.getEffectiveCalories();

            Integer origP = item.getProtein();
            Integer actP =
                    item.getModifiedProtein() != null
                            ? item.getModifiedProtein()
                            : origP;

            Integer origC = item.getCarbs();
            Integer actC =
                    item.getModifiedCarbs() != null
                            ? item.getModifiedCarbs()
                            : origC;

            Integer origF = item.getFat();
            Integer actF =
                    item.getModifiedFat() != null
                            ? item.getModifiedFat()
                            : origF;

            logItem.update(
                    item.getType(),
                    originalName,
                    item.getEffectiveName(),
                    item.getCalories(),
                    actualCalories,
                    origP,
                    actP,
                    origC,
                    actC,
                    origF,
                    actF,
                    item.getStatus(),
                    item.isModified()
            );

            logItemRepository.save(logItem);
        }

        recalculateLog(logEntity, dailyPlan);
    }

    /**
     * Log 집계 재계산
     */
    private void recalculateLog(Log logEntity, DailyPlan dailyPlan) {

        List<LogItem> logItems =
                logItemRepository.findByLog(logEntity);

        List<PlanItem> allItems =
                planItemRepository.findByDailyPlanId(
                        dailyPlan.getId()
                );

        int consumedCalories = logItems.stream()
                .filter(li ->
                        li.getType() == PlanItemType.MEAL
                                && li.getStatus() == PlanItemStatus.COMPLETED
                )
                .mapToInt(li ->
                        li.getActualCalories() != null
                                ? li.getActualCalories()
                                : 0
                )
                .sum();

        int burnedCalories = logItems.stream()
                .filter(li ->
                        li.getType() == PlanItemType.WORKOUT
                                && li.getStatus() == PlanItemStatus.COMPLETED
                )
                .mapToInt(li ->
                        li.getActualCalories() != null
                                ? li.getActualCalories()
                                : 0
                )
                .sum();

        long completedCount = logItems.stream()
                .filter(li ->
                        li.getStatus() == PlanItemStatus.COMPLETED
                )
                .count();

        long totalActive = allItems.stream()
                .filter(pi ->
                        pi.getStatus() != PlanItemStatus.SKIPPED
                )
                .count();

        float completionRate =
                totalActive > 0
                        ? (float) completedCount / totalActive
                        : 0f;

        logEntity.updateAggregates(
                consumedCalories,
                burnedCalories,
                completionRate
        );

        log.info(
                "[LogService] Recalculated - logId={}, consumed={}, burned={}, rate={}",
                logEntity.getId(),
                consumedCalories,
                burnedCalories,
                completionRate
        );
    }
}