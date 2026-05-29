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
        Role: Expert Personal Trainer (FitRoute AI).
        Output: JSON ONLY. No markdown, no conversational text.
        Language: 모든 운동 이름은 반드시 한국어로 작성할 것. (예: 벤치프레스, 스쿼트, 플랭크)

        Schema:
        {
          "s": {"c": int, "p": int, "cb": int, "f": int},
          "i": [
            {"d": "YYYY-MM-DD", "t": "W", "c": "C|B|L|S|A|CR|CA|R", "n": str, "cal": int, "s": int, "r": int, "w": int}
          ]
        }

        Rules:
        - Generate workout for 7 days from start date.
        - Workouts: 3-5 items per day. Use category codes: C(Chest), B(Back), L(Leg), S(Shoulder), A(Arm), CR(Core), CA(Cardio), R(Rest).
        - For rest day: c="R", n="휴식", all values 0.
        - Provide integers only for all numeric values.
        - Follow the exercise experience level strictly.
        - 운동 이름(n 필드)은 반드시 한국어로 작성.
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
        운동 계획만 생성. 식단 데이터 불필요.
        모든 운동명은 한국어로 작성할 것.
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
      UserProfile profile, List<Food> foods, int calorieTarget) {

    String menu = foods.stream()
        .map(f -> String.format(
            "{\"id\":%d,\"n\":\"%s\",\"cal\":%d,\"c\":\"%s\",\"tag\":\"%s\"}",
            f.getId(),
            f.getName().replace("\"", ""),
            f.getCalories(),
            f.getCategory().name(),
            f.getTags()))
        .collect(Collectors.joining(","));

    // 끼니별 칼로리 배분 계산
    int breakfastTarget = (int) (calorieTarget * 0.3); // 30%
    int lunchTarget = (int) (calorieTarget * 0.4); // 40%
    int dinnerTarget = (int) (calorieTarget * 0.3); // 30%

    return """
        Available foods: [%s]

        오늘 하루 목표 칼로리: %d kcal
        - 아침 목표: %d kcal (±100)
        - 점심 목표: %d kcal (±100)
        - 저녁 목표: %d kcal (±100)

        Diet style: %s, Goal: %s

        Return JSON only:
        {"meals":[{"id":foodId,"meal_type":"BREAKFAST|LUNCH|DINNER"}]}

        Rules:
        - 아침/점심/저녁 각각 2~3개 음식 조합. 총 6~9개 이내.
        - 각 끼니의 칼로리 합이 해당 목표 ±100 이내가 되도록 엄격히 조합할 것
        - 전체 총 칼로리는 반드시 %d kcal ±150 이내
        - 한 끼 구성: 밥류 1개 + 단백질 또는 채소 1~2개
        - "생것", "말린것", "살코기", "분말", "원액" 포함 음식 선택 금지
        """.formatted(
        menu,
        calorieTarget,
        breakfastTarget,
        lunchTarget,
        dinnerTarget,
        mapDiet(profile.getDietStyle()),
        mapGoal(profile.getGoalType()),
        calorieTarget);
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