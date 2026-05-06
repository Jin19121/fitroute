// domain/plan/dto/PlanItemActionRequest.java
package com.fitroute.domain.plan.dto;

import com.fitroute.global.enums.PlanItemStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlanItemActionRequest {

    @NotNull(message = "status는 필수입니다.")
    private PlanItemStatus status;

    // MODIFIED 전용 필드 — 상태가 MODIFIED일 때만 검증
    @Size(max = 200, message = "이름은 200자 이하여야 합니다.")
    private String modifiedName;

    @Min(value = 0, message = "칼로리는 0 이상이어야 합니다.")
    @Max(value = 9999, message = "칼로리는 9999 이하여야 합니다.")
    private Integer modifiedCalories;

    private Integer modifiedProtein;
    private Integer modifiedCarbs;
    private Integer modifiedFat;
    private Integer modifiedSets;
    private Integer modifiedReps;

    /**
     * MODIFIED 상태일 때 최소한 이름 또는 칼로리 중 하나는 필요
     */
    public void validateModifiedFields() {
        if (status == PlanItemStatus.MODIFIED) {
            if (modifiedName == null && modifiedCalories == null) {
                throw new IllegalArgumentException(
                        "MODIFIED 상태에서는 modifiedName 또는 modifiedCalories가 필요합니다.");
            }
        }
    }
}