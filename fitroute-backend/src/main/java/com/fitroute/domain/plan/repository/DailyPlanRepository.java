// domain/plan/repository/DailyPlanRepository.java
package com.fitroute.domain.plan.repository;

import com.fitroute.domain.plan.entity.DailyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPlanRepository extends JpaRepository<DailyPlan, Long> {

    // ─── 레거시 호환 (ACTIVE 플랜 조회 용도로 사용 권장) ────────────────
    Optional<DailyPlan> findByUserIdAndPlanDate(Long userId, LocalDate planDate);

    // ─── 버전 인식 조회 ──────────────────────────────────────────────────

    /** 특정 날짜의 ACTIVE 플랜 조회 */
    Optional<DailyPlan> findByUserIdAndPlanDateAndStatus(
            Long userId, LocalDate planDate, DailyPlan.PlanStatus status);

    /** 최신 버전 기준 단일 플랜 조회 (버전 무관) */
    Optional<DailyPlan> findTopByUserIdAndPlanDateOrderByVersionDesc(
            Long userId, LocalDate planDate);

    /** 특정 날짜의 모든 버전 목록 (최신 버전 우선) */
    List<DailyPlan> findByUserIdAndPlanDateOrderByVersionDesc(
            Long userId, LocalDate planDate);

    // ─── 상태 조건 ───────────────────────────────────────────────────────
    boolean existsByUserIdAndPlanDateAndStatus(
            Long userId, LocalDate planDate, DailyPlan.PlanStatus status);

    // ─── 범위 조회 (캘린더 / 리포트) ────────────────────────────────────
    List<DailyPlan> findByUserIdAndPlanDateBetween(
            Long userId, LocalDate from, LocalDate to);
}