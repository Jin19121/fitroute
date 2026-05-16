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

@RestController
@RequestMapping("/api/weight")
@RequiredArgsConstructor
public class WeightLogController {

    private final WeightLogService weightLogService;

    /** POST /api/weight/today — 오늘 체중 기록 (upsert) */
    @PostMapping("/today")
    public ResponseEntity<WeightLogResponse> logToday(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody WeightLogRequest req) {
        return ResponseEntity.ok(weightLogService.logTodayWeight(userId, req));
    }

    /** GET /api/weight/today — 오늘 체중 조회 */
    @GetMapping("/today")
    public ResponseEntity<WeightLogResponse> getToday(
            @AuthenticationPrincipal Long userId) {
        return weightLogService.getTodayWeight(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}