package com.fitroute.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExerciseExperience {
    BEGINNER("초보"), INTERMEDIATE("중급"), ADVANCED("고급");

    private final String description;
}