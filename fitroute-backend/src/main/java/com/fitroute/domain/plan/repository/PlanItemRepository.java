// domain/plan/repository/PlanItemRepository.java
package com.fitroute.domain.plan.repository;

import com.fitroute.domain.plan.entity.PlanItem;
import com.fitroute.global.enums.PlanItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PlanItemRepository extends JpaRepository<PlanItem, Long> {

  List<PlanItem> findByDailyPlanId(Long dailyPlanId);

  // ★ 추가 — 월간 리포트 배치 조회용 (N+1 방지)
  // DailyPlan ID 목록으로 해당 월의 모든 PlanItem을 한 번에 가져옴
  List<PlanItem> findByDailyPlanIdIn(List<Long> dailyPlanIds);

  @Query("""
      SELECT pi FROM PlanItem pi
      WHERE pi.dailyPlan.id = :planId
        AND pi.date = :date
      ORDER BY pi.type, pi.category
      """)
  List<PlanItem> findByPlanIdAndDate(
      @Param("planId") Long planId,
      @Param("date") LocalDate date);

  @Query("""
      SELECT pi FROM PlanItem pi
      WHERE pi.dailyPlan.id = :planId
        AND pi.date = :date
        AND pi.type = :type
      """)
  List<PlanItem> findByPlanIdAndDateAndType(
      @Param("planId") Long planId,
      @Param("date") LocalDate date,
      @Param("type") PlanItemType type);

  @Query("""
      SELECT COUNT(pi) FROM PlanItem pi
      WHERE pi.dailyPlan.id = :planId
        AND pi.date BETWEEN :start AND :end
        AND pi.status = 'COMPLETED'
      """)
  long countCompletedByPlanIdAndDateBetween(
      @Param("planId") Long planId,
      @Param("start") LocalDate start,
      @Param("end") LocalDate end);

  @Query("""
      SELECT COUNT(pi) FROM PlanItem pi
      WHERE pi.dailyPlan.id = :planId
        AND pi.date BETWEEN :start AND :end
        AND pi.status <> 'SKIPPED'
      """)
  long countActiveByPlanIdAndDateBetween(
      @Param("planId") Long planId,
      @Param("start") LocalDate start,
      @Param("end") LocalDate end);
}