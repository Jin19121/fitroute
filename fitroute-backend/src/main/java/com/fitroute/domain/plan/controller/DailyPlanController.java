// domain/plan/controller/DailyPlanController.java
package com.fitroute.domain.plan.controller;

import com.fitroute.domain.plan.dto.DailyPlanResponse;
import com.fitroute.domain.plan.service.DailyPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class DailyPlanController {

    private final DailyPlanService dailyPlanService;

    /**
     * POST /api/plans/today/generate
     * 온보딩 완료 직후 호출 — 오늘 하루치 계획 생성.
     * 이미 있으면 기존 계획 반환.
     */
    @PostMapping("/today/generate")
    public ResponseEntity<DailyPlanResponse> generate(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(dailyPlanService.generateTodayPlan(userId));
    }

    /**
     * GET /api/plans/today
     * 대시보드에서 오늘 계획 조회.
     * 계획이 없으면 status="NO_PLAN" 반환.
     */
    @GetMapping("/today")
    public ResponseEntity<DailyPlanResponse> getToday(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(dailyPlanService.getTodayPlan(userId));
    }
}