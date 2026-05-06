package com.fitroute.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GoalType {
    WEIGHT_LOSS("체중 감량"), MUSCLE_GAIN("근육 증가"), MAINTENANCE("유지");

    private final String description;
}