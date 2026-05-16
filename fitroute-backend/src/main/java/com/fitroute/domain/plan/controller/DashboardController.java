// domain/plan/controller/DashboardController.java
package com.fitroute.domain.plan.controller;

import com.fitroute.domain.plan.dto.DashboardResponse;
import com.fitroute.domain.plan.dto.PlanItemActionRequest; // ← 변경
import com.fitroute.domain.plan.service.DashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard/today")
    public ResponseEntity<DashboardResponse> getTodayDashboard(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(dashboardService.getDashboard(userId));
    }

    @PatchMapping("/plans/items/{itemId}/action")
    public ResponseEntity<Void> applyItemAction(
            @PathVariable Long itemId,
            @Valid @RequestBody PlanItemActionRequest request,
            @AuthenticationPrincipal Long userId) {
        dashboardService.applyItemAction(itemId, userId, request);
        return ResponseEntity.noContent().build();
    }
}