// domain/plan/controller/DailyPlanController.java
package com.fitroute.domain.plan.controller;

import com.fitroute.domain.plan.dto.DailyPlanResponse;
import com.fitroute.domain.plan.dto.PlanItemCreateRequest;
import com.fitroute.domain.plan.dto.PlanItemResponse;
import com.fitroute.domain.plan.dto.WeeklyWorkoutPlanResponse;
import com.fitroute.domain.plan.service.DailyPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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
    public ResponseEntity<DailyPlanResponse> generate(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(dailyPlanService.generateTodayPlan(userId));
    }

    /**
     * GET /api/plans/today
     * 대시보드에서 오늘 계획 조회.
     * 계획이 없으면 status="NO_PLAN" 반환.
     */
    @GetMapping("/today")
    public ResponseEntity<DailyPlanResponse> getToday(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(dailyPlanService.getTodayPlan(userId));
    }

    /**
     * POST /api/plans/items
     * 운동 또는 식단 항목 직접 추가
     */
    @PostMapping("/items")
    public ResponseEntity<PlanItemResponse> addPlanItem(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PlanItemCreateRequest request) {
        return ResponseEntity.status(201)
                .body(dailyPlanService.addPlanItem(userId, request));
    }

    /**
     * GET /api/plans/workout/weekly
     * 주간 운동 계획 조회
     * startDate 미지정 시 이번 주 월요일부터 자동 계산
     */
    @GetMapping("/workout/weekly")
    public ResponseEntity<List<WeeklyWorkoutPlanResponse>> getWeeklyWorkoutPlan(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String startDate) {

        LocalDate start = (startDate == null || startDate.isBlank())
                ? LocalDate.now().with(java.time.DayOfWeek.MONDAY)
                : LocalDate.parse(startDate);

        return ResponseEntity.ok(dailyPlanService.getWeeklyWorkoutPlan(userId, start));
    }
}