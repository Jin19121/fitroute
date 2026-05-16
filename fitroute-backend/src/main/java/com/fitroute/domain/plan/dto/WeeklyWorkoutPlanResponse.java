// domain/plan/dto/WeeklyWorkoutPlanResponse.java
package com.fitroute.domain.plan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyWorkoutPlanResponse {

    private LocalDate date;
    private String dayName; // 월, 화, 수...
    private List<RoutineBlockDto> routines;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoutineBlockDto {
        private Long id;
        private String name;
        private Integer duration; // 분
        private String category; // CHEST, BACK 등
        private List<ExerciseDto> exercises;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class ExerciseDto {
            private String name;
            private Integer sets;
            private Integer reps;
        }
    }
}