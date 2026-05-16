// domain/weight/dto/WeightLogRequest.java
package com.fitroute.domain.weight.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WeightLogRequest {

    @NotNull(message = "체중은 필수입니다.")
    @DecimalMin(value = "20.0", message = "체중은 20kg 이상이어야 합니다.")
    @DecimalMax(value = "300.0", message = "체중은 300kg 이하여야 합니다.")
    private Float weight;

    @DecimalMin(value = "0.0", message = "체지방률은 0% 이상이어야 합니다.")
    @DecimalMax(value = "70.0", message = "체지방률은 70% 이하여야 합니다.")
    private Float bodyFatPct;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "200.0")
    private Float muscleMass;
}