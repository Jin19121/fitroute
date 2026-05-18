// domain/plan/service/PlanService.java
package com.fitroute.domain.plan.service;

import com.fitroute.domain.plan.dto.PlanStatusResponse;
import com.fitroute.domain.plan.repository.PlanRepository;
import com.fitroute.domain.plan.entity.PlanStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private final PlanRepository planRepository;
    private final PlanGenerationService planGenerationService;

    public PlanStatusResponse getPlanStatus(Long userId) {
        return planRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(plan -> new PlanStatusResponse(plan.getStatus()))
                .orElse(new PlanStatusResponse(PlanStatus.GENERATING));
    }

    @Transactional
    public void triggerGeneration(Long userId) {
        planGenerationService.generateForUser(userId);
    }
}