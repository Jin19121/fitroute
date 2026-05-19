// src/main/java/com/fitroute/domain/plan/dto/PlanItemActionRequest.java
package com.fitroute.domain.plan.dto;

import com.fitroute.global.enums.PlanItemAction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlanItemActionRequest {

    // ★ PHASE 2 변경: status 필드를 제거하고 action 필드로 전면 교체
    @NotNull(message = "action은 필수입니다.")
    private PlanItemAction action;

    // MODIFIED/EDITED 계열 필드 보존
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
     * ★ action 기반 커스텀 유효성 검증
     * MODIFY 또는 COMPLETE_WITH_MODIFY 상태일 때 최소한 이름 또는 칼로리 중 하나는 필수적으로 제공되어야 합니다.
     */
    public void validateModifiedFields() {
        if (action == PlanItemAction.MODIFY || action == PlanItemAction.COMPLETE_WITH_MODIFY) {
            if (modifiedName == null && modifiedCalories == null) {
                throw new IllegalArgumentException(
                        "MODIFY 또는 COMPLETE_WITH_MODIFY 액션 시에는 modifiedName 또는 modifiedCalories 중 하나 이상이 필수입니다.");
            }
        }
    }
}