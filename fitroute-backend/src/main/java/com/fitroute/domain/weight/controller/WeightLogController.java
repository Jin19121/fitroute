// domain/weight/controller/WeightLogController.java
package com.fitroute.domain.weight.controller;

import com.fitroute.domain.weight.dto.WeightLogRequest;
import com.fitroute.domain.weight.dto.WeightLogResponse;
import com.fitroute.domain.weight.service.WeightLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/weight-logs")
@RequiredArgsConstructor
public class WeightLogController {

    private final WeightLogService weightLogService;

    /**
     * POST /api/weight-logs
     * 체중 기록 (같은 날짜면 upsert)
     */
    @PostMapping
    public ResponseEntity<WeightLogResponse> record(
            @Valid @RequestBody WeightLogRequest req,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(weightLogService.recordWeight(userId, req));
    }

    /**
     * GET /api/weight-logs?year=2025&month=6
     * 월별 체중 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<WeightLogResponse>> getMonthly(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(weightLogService.getMonthlyLogs(userId, year, month));
    }

    /**
             * DELETE /api/weight-logs/{date}
     * 특정 날짜 체중 삭제
     */
    @DeleteMapping("/{date}")
    public ResponseEntity<Void> delete(
            @PathVariable LocalDate date,
            @AuthenticationPrincipal Long userId) {
        weightLogService.deleteWeight(userId, date);
        return ResponseEntity.noContent().build();
    }
}