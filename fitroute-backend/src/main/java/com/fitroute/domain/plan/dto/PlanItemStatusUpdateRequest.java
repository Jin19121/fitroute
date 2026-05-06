// domain/plan/dto/PlanItemStatusUpdateRequest.java
package com.fitroute.domain.plan.dto;

import com.fitroute.global.enums.PlanItemStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlanItemStatusUpdateRequest {

    @NotNull(message = "status는 필수입니다.")
    private PlanItemStatus status;
}