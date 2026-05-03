// global/enums/ActivityLevel.java

package com.fitroute.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityLevel {
    SEDENTARY("거의 운동 안 함", 1.2),
    LIGHTLY_ACTIVE("가벼운 운동 (주 1~3회)", 1.375),
    MODERATELY_ACTIVE("중간 강도 운동 (주 3~5회)", 1.55),
    VERY_ACTIVE("고강도 운동 (주 6~7회)", 1.725),
    EXTRA_ACTIVE("매우 고강도 또는 육체노동", 1.9);

    private final String description;
    private final double multiplier;
}