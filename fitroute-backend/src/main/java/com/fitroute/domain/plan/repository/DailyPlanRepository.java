// domain/plan/repository/DailyPlanRepository.java
package com.fitroute.domain.plan.repository;

import com.fitroute.domain.plan.entity.DailyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPlanRepository extends JpaRepository<DailyPlan, Long> {

    // 특정 날짜 플랜 조회 (대시보드)
    Optional<DailyPlan> findByUserIdAndPlanDate(Long userId, LocalDate planDate);

    // FAILED 상태 플랜 존재 여부 (대시보드 "계획 짜기" 버튼 노출 조건)
    boolean existsByUserIdAndPlanDateAndStatus(Long userId, LocalDate planDate, DailyPlan.PlanStatus status);

    // 날짜 범위 조회 (캘린더)
    List<DailyPlan> findByUserIdAndPlanDateBetween(Long userId, LocalDate from, LocalDate to);
}