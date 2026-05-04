// src/main/java/com/fitroute/global/ai/GeminiPromptBuilder.java
package com.fitroute.global.ai;

import com.fitroute.domain.user.entity.UserProfile;
import com.fitroute.global.util.CalorieCalculator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

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
   * 사용자의 구체적인 신체 데이터 및 계산된 타겟 값 전달 (User Prompt)
   */
  public String buildInitialPlanPrompt(UserProfile profile) {
    int cal = calculateTargetCalories(profile);
    String start = LocalDate.now().toString();

    return """
        Input Data:
        u={gender:%s, height:%d, weight:%.1f, target_weight:%.1f, period:%d, goal:%s, activity:%s, diet:%s, experience:%s}
        target_calories=%d
        start_date=%s
        """
        .formatted(
            mapGender(profile.getGender()),
            profile.getHeight().intValue(),
            profile.getWeight(),
            profile.getTargetWeight(),
            profile.getTargetPeriod(),
            mapGoal(profile.getGoalType()),
            mapActivity(profile.getActivityLevel()),
            mapDiet(profile.getDietStyle()),
            mapExperience(profile.getExerciseExperience()),
            cal,
            start);
  }

  private int calculateTargetCalories(UserProfile profile) {
    double bmr = CalorieCalculator.calculateBMR(profile);
    double tdee = CalorieCalculator.calculateTDEE(bmr, profile.getActivityLevel());
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
      case "WEIGHT_LOSS" -> "CUT"; // 수정
      case "MUSCLE_GAIN" -> "BULK"; // 수정
      default -> "BAL";
    };
  }

  private String mapActivity(Object activity) {
    if (activity == null)
      return "M";
    return switch (activity.toString()) {
      case "SEDENTARY" -> "S"; // 수정
      case "LIGHTLY_ACTIVE" -> "L"; // 수정
      case "MODERATELY_ACTIVE" -> "M"; // 수정
      case "VERY_ACTIVE" -> "H"; // 수정
      case "EXTRA_ACTIVE" -> "VH"; // 수정
      default -> "M";
    };
  }

  private String mapDiet(Object diet) {
    if (diet == null)
      return "B";
    return switch (diet.toString()) {
      case "LOW_CALORIE" -> "LC"; // 수정
      case "LOW_CARB_HIGH_PROTEIN" -> "LCP"; // 수정
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