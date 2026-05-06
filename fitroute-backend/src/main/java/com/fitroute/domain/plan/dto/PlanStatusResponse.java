// domain/plan/dto/PlanStatusResponse.java
package com.fitroute.domain.plan.dto;

import com.fitroute.domain.plan.entity.PlanStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlanStatusResponse {
    private PlanStatus status; // GENERATING, ACTIVE, FAILED
}