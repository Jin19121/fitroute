package com.fitroute.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    @GetMapping("/summary")
    public Map<String, Object> getTodaySummary() {
        return Map.of(
            "message", "핏루트 대시보드에 오신 걸 환영합니다! 🏋️‍♂️",
            "today", Map.of(
                "exercise", "어깨 운동 (3세트 남음)",
                "diet", "점심 닭가슴살 샐러드 예정",
                "weight", "75.5kg (입력 완료)"
            ),
            "status", "Keep Going!"
        );
    }
}