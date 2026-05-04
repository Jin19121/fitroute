package com.fitroute.domain.plan.repository;

import com.fitroute.domain.plan.entity.PlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlanItemRepository extends JpaRepository<PlanItem, Long> {
    List<PlanItem> findByPlanId(Long planId);
}