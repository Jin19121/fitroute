// domain/plan/repository/DailyPlanRepository.java
package com.fitroute.domain.plan.repository;

import com.fitroute.domain.plan.entity.DailyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface DailyPlanRepository extends JpaRepository<DailyPlan, Long> {
    Optional<DailyPlan> findByUserIdAndPlanDate(Long userId, LocalDate date);

    boolean existsByUserIdAndPlanDate(Long userId, LocalDate date);
}