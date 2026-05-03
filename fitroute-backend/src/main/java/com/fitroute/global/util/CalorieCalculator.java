package com.fitroute.global.util;

import com.fitroute.domain.user.entity.UserProfile;
import com.fitroute.global.enums.ActivityLevel;
import com.fitroute.global.enums.Gender;

import java.time.LocalDate;
import java.time.Period;

public class CalorieCalculator {

    private CalorieCalculator() {
    }

    public static double calculateBMR(UserProfile profile) {
        int age = Period.between(profile.getBirthDate(), LocalDate.now()).getYears();

        if (profile.getGender() == Gender.MALE) {
            return 88.362
                    + (13.397 * profile.getWeight())
                    + (4.799 * profile.getHeight())
                    - (5.677 * age);
        } else {
            return 447.593
                    + (9.247 * profile.getWeight())
                    + (3.098 * profile.getHeight())
                    - (4.330 * age);
        }
    }

    public static double calculateTDEE(double bmr, ActivityLevel level) {
        return bmr * level.getMultiplier();
    }

    public static int calculateTargetCalories(
            double tdee,
            float currentWeight,
            float targetWeight,
            int targetPeriodWeeks,
            Gender gender) {
        float weightDiff = currentWeight - targetWeight;

        // 증량 또는 유지가 목표면 TDEE 그대로 반환
        if (weightDiff <= 0) {
            return (int) Math.round(tdee);
        }

        // 1kg 감량 = 약 7,700 kcal 적자 필요
        double weeklyDeficitKcal = (weightDiff / targetPeriodWeeks) * 7_700.0;
        double dailyDeficit = weeklyDeficitKcal / 7.0;

        double target = tdee - dailyDeficit;

        // 성별 기준 최소 칼로리 (안전 하한선)
        double lowerBound = (gender == Gender.MALE) ? 1_500.0 : 1_200.0;

        // 상한: TDEE의 85% (너무 극단적인 적자 방지)
        double upperBound = tdee * 0.85;

        return (int) Math.round(Math.max(lowerBound, Math.min(target, upperBound)));
    }
}