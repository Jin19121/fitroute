// domain/weight/dto/WeightLogRequest.java
package com.fitroute.domain.weight.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class WeightLogRequest {

    @NotNull(message = "측정 날짜는 필수입니다.")
    @PastOrPresent(message = "측정 날짜는 오늘 이전이어야 합니다.")
    private LocalDate logDate; // ★ measuredAt → logDate

    @NotNull(message = "체중은 필수입니다.")
    @DecimalMin(value = "20.0", message = "체중은 20kg 이상이어야 합니다.")
    @DecimalMax(value = "300.0", message = "체중은 300kg 이하여야 합니다.")
    private Float weightKg;

    @DecimalMin(value = "0.0", message = "체지방률은 0% 이상이어야 합니다.")
    @DecimalMax(value = "70.0", message = "체지방률은 70% 이하여야 합니다.")
    private Float bodyFatPct; // ★ 추가

    @DecimalMin(value = "0.0", message = "골격근량은 0kg 이상이어야 합니다.")
    @DecimalMax(value = "200.0", message = "골격근량은 200kg 이하여야 합니다.")
    private Float muscleMass; // ★ 추가

    @Size(max = 200, message = "메모는 200자 이하여야 합니다.")
    private String note;
}