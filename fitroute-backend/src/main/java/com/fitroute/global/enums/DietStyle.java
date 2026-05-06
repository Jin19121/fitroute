package com.fitroute.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DietStyle {
    BALANCED("일반식"), LOW_CALORIE("다이어트식"), LOW_CARB_HIGH_PROTEIN("저탄수/고단백");

    private final String description;
}