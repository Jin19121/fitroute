// src/main/java/com/fitroute/global/ai/GeminiPromptBuilder.java
package com.fitroute.global.ai;

import com.fitroute.domain.food.entity.Food;
import com.fitroute.domain.user.entity.UserProfile;
import com.fitroute.global.util.CalorieCalculator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GeminiPromptBuilder {

  /**
   * AI의 역할, 페르소나, 출력 스키마 및 엄격한 규칙 정의 (System Instruction)
   */
  public String getPlanSystemInstruction() {
    return """
        Role: Expert Personal Trainer & Nutritionist (FitRoute AI).
        Output: JSON ONLY. No markdown, no conversational text.

        Schema:
        {
          "s": {"c": int, "p": int, "cb": int, "f": int},
          "i": [
            {"d": "YYYY-MM-DD", "t": "M", "c": "B|L|D|S", "n": str, "cal": int, "p": int, "cb": int, "f": int},
            {"d": "YYYY-MM-DD", "t": "W", "c": "C|B|L|S|A|CR|CA|R", "n": str, "cal": int, "s": int, "r": int, "w": int}
          ]
        }

        Rules:
        - Generate for 7 days from start date.
        - Daily calories sum must be target calories (±100).
        - Meals: B(Breakfast), L(Lunch), D(Dinner) required. S(Snack) optional.
        - Workouts: 4-10 items per day. Use category codes: C(Chest), B(Back), L(Leg), S(Shoulder), A(Arm), CR(Core), CA(Cardio), R(Rest).
        - For rest day: c="R", n="Rest", all values 0.
        - Provide integers only for all numeric values.
        - Follow the exercise experience level strictly.
        """;
  }

  /**
   * 운동 계획 생성 프롬프트
   * (운동은 AI 자유 생성 / 식단 데이터 제외)
   */
  public String buildWorkoutPrompt(UserProfile profile) {
    int cal = calculateTargetCalories(profile);
    String start = LocalDate.now().toString();

    return """
        Input Data:
        u={gender:%s, height:%d, weight:%.1f, target_weight:%.1f, period:%d, goal:%s, activity:%s, experience:%s}
        target_calories=%d
        start_date=%s
        Generate workout plan only. No meal data.
        """
        .formatted(
            mapGender(profile.getGender()),
            profile.getHeight().intValue(),
            profile.getWeight(),
            profile.getTargetWeight(),
            profile.getTargetPeriod(),
            mapGoal(profile.getGoalType()),
            mapActivity(profile.getActivityLevel()),
                mapExperience(profile.getExerciseExperience()),
            cal,
            start);
  }

  /**
   * 식단 선택 프롬프트
   * (AI가 DB 메뉴판에서 음식 ID만 선택)
   */
  public String buildMealSelectionPrompt(
      UserProfile profile,
      List<Food> foods,
      int calorieTarget) {

    // 메뉴판 직렬화 — 최소 토큰 최적화
    String menu = foods.stream()
        .map(f -> String.format(
            "{\"id\":%d,\"n\":\"%s\",\"cal\":%d,\"c\":\"%s\",\"tag\":\"%s\"}",
            f.getId(),
            f.getName().replace("\"", ""),
            f.getCalories(),
            f.getCategory().name(),
            f.getTags()))
        .collect(Collectors.joining(","));

    return """
        Available foods: [%s]
        Target calories: %d
        Diet style: %s, Goal: %s

        Select food IDs for today's 3 meals.

        Return JSON only:
        {"meals":[
          {"id":foodId,"meal_type":"BREAKFAST"},
          {"id":foodId,"meal_type":"LUNCH"},
          {"id":foodId,"meal_type":"DINNER"}
        ]}

        Rules:
        - BREAKFAST + LUNCH + DINNER 각 1개씩 필수
        - 총 칼로리가 target ±150 이내
        - diet style 태그와 일치하는 음식 우선 선택
        """
        .formatted(
            menu,
            calorieTarget,
            mapDiet(profile.getDietStyle()),
            mapGoal(profile.getGoalType()));
  }

  private int calculateTargetCalories(UserProfile profile) {
    double bmr = CalorieCalculator.calculateBMR(profile);
    double tdee = CalorieCalculator.calculateTDEE(
        bmr,
        profile.getActivityLevel());

    return CalorieCalculator.calculateTargetCalories(
        tdee,
        profile.getWeight(),
        profile.getTargetWeight(),
        profile.getTargetPeriod(),
        profile.getGender());
  }

  // =========================
  // ENUM 압축 매핑 (토큰 절감 핵심)
  // =========================

  private String mapGender(Object gender) {
    if (gender == null)
      return "M";

    return gender.toString().startsWith("M") ? "M" : "F";
  }

  private String mapGoal(Object goal) {
    if (goal == null)
      return "BAL";

    return switch (goal.toString()) {
      case "WEIGHT_LOSS" -> "CUT";
      case "MUSCLE_GAIN" -> "BULK";
      default -> "BAL";
    };
  }

  private String mapActivity(Object activity) {
    if (activity == null)
      return "M";

    return switch (activity.toString()) {
      case "SEDENTARY" -> "S";
      case "LIGHTLY_ACTIVE" -> "L";
      case "MODERATELY_ACTIVE" -> "M";
      case "VERY_ACTIVE" -> "H";
      case "EXTRA_ACTIVE" -> "VH";
      default -> "M";
    };
  }

  private String mapDiet(Object diet) {
    if (diet == null)
      return "B";

    return switch (diet.toString()) {
      case "LOW_CALORIE" -> "LC";
      case "LOW_CARB_HIGH_PROTEIN" -> "LCP";
      default -> "B";
    };
  }

  private String mapExperience(Object exp) {
    if (exp == null)
      return "B";

    return switch (exp.toString()) {
      case "BEGINNER" -> "B";
      case "INTERMEDIATE" -> "I";
      case "ADVANCED" -> "A";
      default -> "B";
    };
  }
}