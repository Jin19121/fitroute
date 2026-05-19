// src/main/java/com/fitroute/domain/plan/dto/PlanItemResponse.java
package com.fitroute.domain.plan.dto;

import com.fitroute.domain.plan.entity.PlanItem;
import com.fitroute.global.enums.PlanItemCategory;
import com.fitroute.global.enums.PlanItemStatus;
import com.fitroute.global.enums.PlanItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanItemResponse {

    private Long id;

    private PlanItemType type;

    private PlanItemCategory category;

    private LocalDate date;

    // ─── MEAL ──────────────────────────────────
    private String foodName;
    private Integer protein;
    private Integer carbs;
    private Integer fat;

    // ─── WORKOUT ──────────────────────────────
    private String exerciseName;
    private Integer sets;
    private Integer reps;
    private Integer weightKg;
    private Integer durationMin;

    // ─── 공통 ──────────────────────────────────
    private Integer calories;

    private PlanItemStatus status;

    // ★ PHASE 2 추가: 수정 플래그 단독 제공 (프론트가 UI 렌더링 시 조건부 텍스트/뱃지 처리에 활용)
    private boolean isModified;

    // ─── 수정된 값 ──────────────────────────────
    private String effectiveName;
    private Integer effectiveCalories;

    // ─── Factory 메서드 ────────────────────────
    public static PlanItemResponse from(PlanItem item) {
        return PlanItemResponse.builder()
                .id(item.getId())
                .type(item.getType())
                .category(item.getCategory())
                .date(item.getDate())
                .foodName(item.getFoodName())
                .protein(item.getProtein())
                .carbs(item.getCarbs())
                .fat(item.getFat())
                .exerciseName(item.getExerciseName())
                .sets(item.getSets())
                .reps(item.getReps())
                .weightKg(item.getWeightKg())
                .durationMin(item.getDurationMin())
                .calories(item.getCalories())
                .status(item.getStatus())
                .isModified(item.isModified()) // ★ 엔티티의 boolean 분리 값 매핑
                .effectiveName(item.getEffectiveName())
                .effectiveCalories(item.getEffectiveCalories())
                .build();
    }
}