// domain/report/controller/ReportController.java
package com.fitroute.domain.report.controller;

import com.fitroute.domain.report.dto.DailyReportResponse;
import com.fitroute.domain.report.dto.MonthlyReportResponse;
import com.fitroute.domain.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * GET /api/reports/monthly?year=2025&month=6
     * 월간 캘린더 + KPI 요약 조회
     */
    @GetMapping("/monthly")
    public ResponseEntity<MonthlyReportResponse> getMonthly(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal Long userId) {

        validateYearMonth(year, month);
        return ResponseEntity.ok(reportService.getMonthlyReport(userId, year, month));
    }

    /**
     * GET /api/reports/daily/{date}
     * 특정 날짜 클릭 시 상세 카드 조회
     */
    @GetMapping("/daily/{date}")
    public ResponseEntity<DailyReportResponse> getDaily(
            @PathVariable LocalDate date,
            @AuthenticationPrincipal Long userId) {

        return ResponseEntity.ok(reportService.getDayReport(userId, date));
    }

    // year/month 범위 검증 — 서비스에 잘못된 값이 내려가지 않도록 컨트롤러에서 사전 차단
    private void validateYearMonth(int year, int month) {
        if (year < 2020 || year > 2100) {
            throw new IllegalArgumentException("year는 2020~2100 사이여야 합니다.");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month는 1~12 사이여야 합니다.");
        }
    }
}