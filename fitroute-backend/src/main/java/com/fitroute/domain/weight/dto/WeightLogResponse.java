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
    private LocalDate measuredAt;
    private Float weightKg;
    private String note;

    public static WeightLogResponse from(WeightLog log) {
        return WeightLogResponse.builder()
                .id(log.getId())
                .measuredAt(log.getMeasuredAt())
                .weightKg(log.getWeightKg())
                .note(log.getNote())
                .build();
    }
}