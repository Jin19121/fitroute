// src/main/java/com/fitroute/global/ai/GeminiResponseParser.java
package com.fitroute.global.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitroute.domain.plan.entity.DailyPlan;
import com.fitroute.domain.plan.entity.PlanItem;
import com.fitroute.global.enums.PlanItemCategory;
import com.fitroute.global.enums.PlanItemType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiResponseParser {

    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────
    // Gemini Raw Response Text 추출
    // ─────────────────────────────────────────────

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
            throw new IllegalStateException(
                    "Gemini 응답 텍스트 추출 실패: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Daily Calories
    // ─────────────────────────────────────────────

    public int parseDailyCalories(String planJson) {
        try {
            JsonNode root = objectMapper.readTree(planJson);
            return root.path("s").path("c").asInt();

        } catch (Exception e) {
            log.warn("dailyCalories 파싱 실패, 기본값 사용");
            return 0;
        }
    }

    // ─────────────────────────────────────────────
    // Meal Plan Parsing
    // ─────────────────────────────────────────────

    public Map<String, Object> parseMealPlan(String planJson) {

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();

        result.put("breakfast", new ArrayList<>());
        result.put("lunch", new ArrayList<>());
        result.put("dinner", new ArrayList<>());
        result.put("snack", new ArrayList<>());

        try {

            JsonNode root = objectMapper.readTree(planJson);

            for (JsonNode item : root.path("i")) {

                if (!"M".equals(item.path("t").asText()))
                    continue;

                String category = switch (item.path("c").asText()) {
                    case "B" -> "breakfast";
                    case "L" -> "lunch";
                    case "D" -> "dinner";
                    case "S" -> "snack";
                    default -> null;
                };

                if (category == null)
                    continue;

                Map<String, Object> meal = new LinkedHashMap<>();

                meal.put("name", item.path("n").asText());
                meal.put("calories", item.path("cal").asInt());
                meal.put("protein", item.path("p").asInt());
                meal.put("carbs", item.path("cb").asInt());
                meal.put("fat", item.path("f").asInt());

                result.get(category).add(meal);
            }

        } catch (Exception e) {
            throw new IllegalStateException(
                    "mealPlan 파싱 실패: " + e.getMessage());
        }

        return Collections.unmodifiableMap(result);
    }

    // ─────────────────────────────────────────────
    // Workout Plan Parsing
    // ─────────────────────────────────────────────

    public Map<String, Object> parseWorkoutPlan(String planJson) {

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();

        try {

            JsonNode root = objectMapper.readTree(planJson);

            for (JsonNode item : root.path("i")) {

                if (!"W".equals(item.path("t").asText()))
                    continue;

                String category = switch (item.path("c").asText()) {
                    case "C" -> "chest";
                    case "B" -> "back";
                    case "L" -> "legs";
                    case "S" -> "shoulders";
                    case "A" -> "arms";
                    case "CR" -> "core";
                    case "CA" -> "cardio";
                    case "R" -> "rest";
                    default -> null;
                };

                if (category == null)
                    continue;

                result.computeIfAbsent(category, k -> new ArrayList<>());

                Map<String, Object> workout = new LinkedHashMap<>();

                workout.put("name", item.path("n").asText());
                workout.put("calories", item.path("cal").asInt());
                workout.put("sets", item.path("s").asInt());
                workout.put("reps", item.path("r").asInt());
                workout.put("weightKg", item.path("w").asInt());

                result.get(category).add(workout);
            }

        } catch (Exception e) {
            throw new IllegalStateException(
                    "workoutPlan 파싱 실패: " + e.getMessage());
        }

        return Collections.unmodifiableMap(result);
    }

    // ─────────────────────────────────────────────
    // 식단 선택 응답 파싱
    // ─────────────────────────────────────────────

    public record SelectedMealDto(
            Long foodId,
            String mealType) {
    }

    public List<SelectedMealDto> parseMealSelection(
            String planJson) {

        List<SelectedMealDto> result = new ArrayList<>();

        try {

            JsonNode root = objectMapper.readTree(planJson);

            for (JsonNode meal : root.path("meals")) {

                long id = meal.path("id").asLong();
                String mealType = meal.path("meal_type").asText();

                if (id > 0 && !mealType.isBlank()) {
                    result.add(
                            new SelectedMealDto(
                                    id,
                                    mealType));
                }
            }

        } catch (Exception e) {
            log.warn(
                    "[ResponseParser] 식단 선택 파싱 실패: {}",
                    e.getMessage());
        }

        return result;
    }

    // ─────────────────────────────────────────────
    // PlanItem Entity Parsing
    // ─────────────────────────────────────────────

    public List<PlanItem> parsePlanItems(
            String planJson,
            DailyPlan dailyPlan) {

        List<PlanItem> result = new ArrayList<>();

        try {

            JsonNode root = objectMapper.readTree(planJson);

            for (JsonNode item : root.path("i")) {

                try {
                    result.add(parseItem(item, dailyPlan));

                } catch (Exception e) {
                    log.warn(
                            "PlanItem 파싱 스킵: {}",
                            e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException(
                    "PlanItem 리스트 파싱 실패: "
                            + e.getMessage());
        }

        return result;
    }

    private PlanItem parseItem(
            JsonNode item,
            DailyPlan dailyPlan) {

        String typeCode = item.path("t").asText();

        PlanItemType type = typeCode.equals("M")
                ? PlanItemType.MEAL
                : PlanItemType.WORKOUT;

        PlanItemCategory category = parseCategoryCode(
                item.path("c").asText(),
                type);

        LocalDate date = LocalDate.parse(
                item.path("d").asText());

        int calories = item.path("cal").asInt();

        validateCalories(calories, type);

        PlanItem.PlanItemBuilder builder = PlanItem.builder()
                .dailyPlan(dailyPlan)
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
                    .weightKg(item.path("w").asInt());
        }

        return builder.build();
    }

    private PlanItemCategory parseCategoryCode(
            String code,
            PlanItemType type) {

        if (type == PlanItemType.MEAL) {

            return switch (code) {
                case "B" -> PlanItemCategory.BREAKFAST;
                case "L" -> PlanItemCategory.LUNCH;
                case "D" -> PlanItemCategory.DINNER;
                case "S" -> PlanItemCategory.SNACK;

                default ->
                    throw new IllegalArgumentException(
                            "알 수 없는 MEAL category: "
                                    + code);
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

                default ->
                    throw new IllegalArgumentException(
                            "알 수 없는 WORKOUT category: "
                                    + code);
            };
        }
    }

    private void validateCalories(
            int calories,
            PlanItemType type) {

        if (type == PlanItemType.WORKOUT
                && calories == 0)
            return;

        if (calories <= 0) {
            throw new IllegalArgumentException(
                    "calories는 0보다 커야 합니다: "
                            + calories);
        }
    }
}