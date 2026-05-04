// domain/plan/controller/PlanController.java
package com.fitroute.domain.plan.controller;

import com.fitroute.domain.plan.entity.Plan;
import com.fitroute.domain.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanRepository planRepository;

    @GetMapping("/today")
    public ResponseEntity<?> getTodayPlanStatus(@AuthenticationPrincipal Long userId) {
        return planRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(plan -> ResponseEntity.ok(plan))
                .orElse(ResponseEntity.notFound().build());
    }
}