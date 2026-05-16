// domain/plan/service/AiPlanService.java
package com.fitroute.domain.plan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitroute.domain.user.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPlanService {

    @Value("${ai.gemini.api.key}")
    private String apiKey;

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=%s";
    private static final int MAX_TOKENS = 800;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 하루 식단+운동 계획을 JSON으로 생성
     * 주의: AI 응답을 직접 신뢰하지 않고, 반드시 validateAndNormalize()를 거침
     */
    public Map<String, Object> generateDailyPlan(UserProfile profile, int calorieTarget) {
        String prompt = buildPrompt(profile, calorieTarget);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Gemini는 헤더 인증 없이 URL 파라미터로 키 전달

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "maxOutputTokens", MAX_TOKENS,
                        "temperature", 0.3 // 낮을수록 JSON 형식 준수율 높아짐
                ));

        try {
            String url = String.format(API_URL, apiKey);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            String rawJson = extractContent(response.getBody());
            return validateAndNormalize(rawJson);

        } catch (Exception e) {
            log.error("Gemini 계획 생성 실패: {}", e.getMessage());
            return buildFallbackPlan(calorieTarget);
        }
    }

    private String buildPrompt(UserProfile profile, int calorieTarget) {
            return String.format(
                            """
                                                            당신은 전문 영양사이자 트레이너입니다. 아래 사용자 정보를 바탕으로 오늘 하루 식단과 운동 계획을 JSON 형식으로만 반환하세요.

                                                            사용자 정보:
                                                            - 현재 체중: %.1fkg, 목표 체중: %.1fkg
                                                            - 키: %.1fcm
                                                            - 목표 칼로리: %d kcal
                                                            - 활동 수준: %s
                                                            - 운동 경험: %s
                                                            - 식단 스타일: %s

                                                            반드시 아래 JSON 구조만 반환하세요. 설명이나 마크다운 없이 JSON만:
                                                            {
                                                              "meal_plan": {
                                                                "meals": [
                                                                  {
                                                                    "type": "BREAKFAST",
                                                                    "name": "음식명",
                                                                    "kcal": 숫자,
                                                                    "protein": 숫자,
                                                                    "carbs": 숫자,
                                                                    "fat": 숫자,
                                                                    "time": "HH:mm"
                                                                  },
                                                                  {"type": "LUNCH", "name": "음식명", "kcal": 숫자, "protein": 숫자, "carbs": 숫자, "fat": 숫자, "time": "HH:mm"},
                                                                  {"type": "DINNER", "name": "음식명", "kcal": 숫자, "protein": 숫자, "carbs": 숫자, "fat": 숫자, "time": "HH:mm"}
                                                                ],
                                                                "total_kcal": 숫자
                                                              },
                                                              "workout_plan": {
                                                                "workouts": [
                                                                  {
                                                                    "name": "운동명",
                                                                    "category": "CHEST",
                                                                    "sets": 숫자,
                                                                    "reps": 숫자,
                                                                    "duration_min": 숫자,
                                                                    "kcal_burn": 숫자
                                                                  }
                                                                ],
                                                                "total_kcal_burn": 숫자
                                                              }
                                                            }

                                                            category는 반드시 다음 중 하나만 사용하세요:
                                                            CHEST(가슴), BACK(등), LEGS(하체), SHOULDERS(어깨), ARMS(팔), CORE(코어), CARDIO(유산소), REST(휴식)

                                                            유산소 운동(달리기, 걷기, 수영 등)은 sets=1, reps=duration_min으로 설정하세요.
                                            """,
                            profile.getWeight(), profile.getTargetWeight(), profile.getHeight(),
                            calorieTarget,
                            profile.getActivityLevel().getDescription(),
                            profile.getExerciseExperience().getDescription(),
                            profile.getDietStyle().getDescription());
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        // Gemini 응답 구조:
        // { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }] }
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

        String raw = (String) parts.get(0).get("text");

        // Gemini가 가끔 ```json ... ``` 감싸서 반환하므로 제거
        return raw.replaceAll("(?s)```json\\s*", "")
                .replaceAll("```", "")
                .trim();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> validateAndNormalize(String rawJson) throws Exception {
        // JSON 파싱
        Map<String, Object> parsed = objectMapper.readValue(rawJson.trim(), Map.class);

        // 필수 키 검증
        if (!parsed.containsKey("meal_plan") || !parsed.containsKey("workout_plan")) {
            throw new IllegalStateException("AI 응답에 필수 필드 누락");
        }

        Map<String, Object> mealPlan = (Map<String, Object>) parsed.get("meal_plan");
        Map<String, Object> workoutPlan = (Map<String, Object>) parsed.get("workout_plan");

        // meal 칼로리 합산 재계산 (AI 실수 방지)
        List<Map<String, Object>> meals = (List<Map<String, Object>>) mealPlan.get("meals");
        int recalcMealKcal = meals.stream()
                .mapToInt(m -> ((Number) m.getOrDefault("kcal", 0)).intValue())
                .sum();
        mealPlan.put("total_kcal", recalcMealKcal);

        // workout 칼로리 합산 재계산
        List<Map<String, Object>> workouts = (List<Map<String, Object>>) workoutPlan.get("workouts");
        int recalcWorkoutKcal = workouts.stream()
                .mapToInt(w -> ((Number) w.getOrDefault("kcal_burn", 0)).intValue())
                .sum();
        workoutPlan.put("total_kcal_burn", recalcWorkoutKcal);

        return parsed;
    }

    private Map<String, Object> buildFallbackPlan(int calorieTarget) {
        // AI 실패 시 기본 한식 템플릿
        return Map.of(
                "meal_plan", Map.of(
                        "meals", List.of(
                                Map.of("type", "BREAKFAST", "name", "오트밀+바나나", "kcal", 320, "time", "08:00"),
                                Map.of("type", "LUNCH", "name", "닭가슴살 샐러드", "kcal", 450, "time", "12:30"),
                                Map.of("type", "DINNER", "name", "두부 된장국+나물", "kcal", 380, "time", "19:00")),
                        "total_kcal", 1150),
                "workout_plan", Map.of(
                        "workouts", List.of(
                                Map.of("name", "걷기", "duration_min", 30, "kcal_burn", 150)),
                        "total_kcal_burn", 150));
    }
}