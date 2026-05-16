// domain/plan/dto/PlanItemCreateRequest.java
package com.fitroute.domain.plan.dto;

import com.fitroute.global.enums.PlanItemStatus;
import com.fitroute.global.enums.PlanItemCategory;
import com.fitroute.global.enums.PlanItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanItemCreateRequest {

    @NotNull(message = "type은 필수입니다")
    private PlanItemType type;

    @NotNull(message = "category는 필수입니다")
    private PlanItemCategory category;

    @NotBlank(message = "name은 필수입니다")
    private String name;

    @NotNull(message = "calories는 필수입니다")
    @PositiveOrZero(message = "calories는 0 이상이어야 합니다")
    private Integer calories;

    // ─── MEAL 전용 ───────────────────────────────
    private Integer protein;
    private Integer carbs;
    private Integer fat;

    // ─── WORKOUT 전용 ───────────────────────────
    private Integer sets;
    private Integer reps;
    private Integer weightKg;
    private Integer durationMin;

    // ─── Status 필드 (선택사항, 기본값: COMPLETED) ──
    @Builder.Default
    private PlanItemStatus status = PlanItemStatus.COMPLETED;

    // ─── 검증 로직 ──────────────────────────────
    public void validate() {
        if (type == null) {
            throw new IllegalArgumentException("type은 필수입니다 (MEAL 또는 WORKOUT)");
        }

        if (category == null) {
            throw new IllegalArgumentException("category는 필수입니다");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name(음식명 또는 운동명)은 필수입니다");
        }

        if (calories == null || calories < 0) {
            throw new IllegalArgumentException("calories는 필수이며 0 이상이어야 합니다");
        }

        // MEAL 검증
        if (type == PlanItemType.MEAL) {
            if (protein == null || carbs == null || fat == null) {
                throw new IllegalArgumentException("MEAL 타입: protein, carbs, fat은 필수입니다");
            }
        }

        // Status 검증 (필수는 아니지만 null인 경우 방어)
        if (status == null) {
            this.status = PlanItemStatus.COMPLETED;
        }
    }
}