package com.fitroute.global.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitroute.domain.plan.entity.Plan;
import com.fitroute.domain.plan.entity.PlanItem;
import com.fitroute.global.enums.PlanItemCategory;
import com.fitroute.global.enums.PlanItemType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiResponseParser {

    private final ObjectMapper objectMapper;

    /**
     * Gemini HTTP 응답 전체에서 텍스트 추출
     * 응답 구조: candidates[0].content.parts[0].text
     */
    public String extractText(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            return root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            throw new IllegalStateException("Gemini 응답 텍스트 추출 실패: " + e.getMessage());
        }
    }

    /**
     * 추출된 텍스트(JSON 문자열)에서 dailyCalories 파싱
     */
    public int parseDailyCalories(String planJson) {
        try {
            JsonNode root = objectMapper.readTree(planJson);
            return root.path("s").path("c").asInt(); // summary.dailyCalories → s.c
        } catch (Exception e) {
            log.warn("dailyCalories 파싱 실패, 기본값 사용");
            return 0;
        }
    }

    /**
     * 추출된 텍스트(JSON 문자열)에서 PlanItem 리스트 파싱
     */
    public List<PlanItem> parsePlanItems(String planJson, Plan plan) {
        List<PlanItem> result = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(planJson);
            JsonNode items = root.path("i");

            for (JsonNode item : items) {
                try {
                    result.add(parseItem(item, plan));
                } catch (Exception e) {
                    // 개별 아이템 파싱 실패 시 해당 아이템만 스킵
                    log.warn("PlanItem 파싱 스킵: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("PlanItem 리스트 파싱 실패: " + e.getMessage());
        }

        return result;
    }

    private PlanItem parseItem(JsonNode item, Plan plan) {
        // t: "M" or "W"
        String typeCode = item.path("t").asText();
        PlanItemType type = typeCode.equals("M") ? PlanItemType.MEAL : PlanItemType.WORKOUT;

        PlanItemCategory category = parseCategoryCode(item.path("c").asText(), type);
        LocalDate date = LocalDate.parse(item.path("d").asText());
        int calories = item.path("cal").asInt();

        validateCalories(calories, type);

        PlanItem.PlanItemBuilder builder = PlanItem.builder()
                .plan(plan)
                .date(date)
                .type(type)
                .category(category)
                .calories(calories);

        if (type == PlanItemType.MEAL) {
            builder
                    .foodName(item.path("n").asText())
                    .protein(item.path("p").asInt())
                    .carbs(item.path("cb").asInt())
                    .fat(item.path("f").asInt());
        } else {
            builder
                    .exerciseName(item.path("n").asText())
                    .sets(item.path("s").asInt())
                    .reps(item.path("r").asInt())
                    .weight(item.path("w").asInt());
        }

        return builder.build();
    }

    private PlanItemCategory parseCategoryCode(String code, PlanItemType type) {
        if (type == PlanItemType.MEAL) {
            return switch (code) {
                case "B" -> PlanItemCategory.BREAKFAST;
                case "L" -> PlanItemCategory.LUNCH;
                case "D" -> PlanItemCategory.DINNER;
                case "S" -> PlanItemCategory.SNACK;
                default -> throw new IllegalArgumentException("알 수 없는 MEAL category: " + code);
            };
        } else {
            return switch (code) {
                case "C" -> PlanItemCategory.CHEST;
                case "B" -> PlanItemCategory.BACK;
                case "L" -> PlanItemCategory.LEGS;
                case "S" -> PlanItemCategory.SHOULDERS;
                case "A" -> PlanItemCategory.ARMS;
                case "CR" -> PlanItemCategory.CORE;
                case "CA" -> PlanItemCategory.CARDIO;
                case "R" -> PlanItemCategory.REST;
                default -> throw new IllegalArgumentException("알 수 없는 WORKOUT category: " + code);
            };
        }
    }

    private void validateCalories(int calories, PlanItemType type) {
        // REST 타입은 calories = 0 허용
        if (type == PlanItemType.WORKOUT && calories == 0)
            return;
        if (calories <= 0) {
            throw new IllegalArgumentException("calories는 0보다 커야 합니다: " + calories);
        }
    }
}