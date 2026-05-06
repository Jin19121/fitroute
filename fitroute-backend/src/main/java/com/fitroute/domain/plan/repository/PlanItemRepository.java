package com.fitroute.domain.plan.repository;

import com.fitroute.domain.plan.entity.PlanItem;
import com.fitroute.global.enums.PlanItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PlanItemRepository extends JpaRepository<PlanItem, Long> {

    List<PlanItem> findByPlanId(Long planId);

    @Query("SELECT pi FROM PlanItem pi WHERE pi.plan.id = :planId AND pi.date = :date ORDER BY pi.type, pi.category")
    List<PlanItem> findByPlanIdAndDate(@Param("planId") Long planId, @Param("date") LocalDate date);

    @Query("SELECT pi FROM PlanItem pi WHERE pi.plan.id = :planId AND pi.date = :date AND pi.type = :type")
    List<PlanItem> findByPlanIdAndDateAndType(
            @Param("planId") Long planId,
            @Param("date") LocalDate date,
            @Param("type") PlanItemType type);

    // 주간 달성률 계산용 (완료된 아이템 / 전체)
    @Query("SELECT COUNT(pi) FROM PlanItem pi WHERE pi.plan.id = :planId AND pi.date BETWEEN :start AND :end AND pi.status = 'COMPLETED'")
    long countCompletedByPlanIdAndDateBetween(@Param("planId") Long planId, @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("SELECT COUNT(pi) FROM PlanItem pi WHERE pi.plan.id = :planId AND pi.date BETWEEN :start AND :end AND pi.status != 'SKIPPED'")
    long countActiveByPlanIdAndDateBetween(@Param("planId") Long planId, @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}