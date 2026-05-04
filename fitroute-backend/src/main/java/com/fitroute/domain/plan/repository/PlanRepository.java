// src/main/java/com/fitroute/domain/plan/repository/PlanRepository.java
package com.fitroute.domain.plan.repository;

import com.fitroute.domain.plan.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    /**
     * 사용자의 가장 최신 계획을 조회 (생성 중인 플랜 포함)
     * 폴링 API(GET /api/plans/today)에서 사용됨
     */
    Optional<Plan> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 사용자의 현재 활성화된(ACTIVE) 플랜만 조회할 때 사용
     */
    Optional<Plan> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId,
            com.fitroute.domain.plan.entity.PlanStatus status);
}