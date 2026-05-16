// domain/weight/dto/WeightLogResponse.java
package com.fitroute.domain.weight.dto;

import com.fitroute.domain.weight.entity.WeightLog;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class WeightLogResponse {
    private Long id;
    private LocalDate logDate;
    private Float weight;
    private Float bodyFatPct;
    private Float muscleMass;
    private Float changeFromPrev; // 전일 대비 변화량 (null 가능)

    public static WeightLogResponse from(WeightLog log, Float changeFromPrev) {
        return WeightLogResponse.builder()
                .id(log.getId())
                .logDate(log.getLogDate())
                .weight(log.getWeight())
                .bodyFatPct(log.getBodyFatPct())
                .muscleMass(log.getMuscleMass())
                .changeFromPrev(changeFromPrev)
                .build();
    }
}