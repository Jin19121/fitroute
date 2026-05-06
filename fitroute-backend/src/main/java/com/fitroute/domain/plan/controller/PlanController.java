// domain/plan/controller/PlanController.java
package com.fitroute.domain.plan.controller;

import com.fitroute.domain.plan.dto.PlanStatusResponse;
import com.fitroute.domain.plan.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping("/status")
    public ResponseEntity<PlanStatusResponse> getPlanStatus(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(planService.getPlanStatus(userId));
    }

    @PostMapping("/generate")
    public ResponseEntity<Void> generatePlan(@AuthenticationPrincipal Long userId) {
        planService.triggerGeneration(userId);
        return ResponseEntity.accepted().build();
    }

    // // 오늘 플랜 상세 (기존 기능 유지)
    // @GetMapping("/today")
    // public ResponseEntity<PlanDetailResponse>
    // getTodayPlan(@AuthenticationPrincipal Long userId) {
    // return ResponseEntity.ok(planService.getTodayPlan(userId));
    // }
}