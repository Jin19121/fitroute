// domain/weight/service/WeightLogService.java
package com.fitroute.domain.weight.service;

import com.fitroute.domain.weight.dto.WeightLogRequest;
import com.fitroute.domain.weight.dto.WeightLogResponse;
import com.fitroute.domain.weight.entity.WeightLog;
import com.fitroute.domain.weight.repository.WeightLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeightLogService {

    private final WeightLogRepository weightLogRepository;

    /**
     * 체중 기록.
     * 같은 날짜 데이터가 이미 존재하면 업데이트(upsert), 없으면 새로 생성.
     */
    @Transactional
    public WeightLogResponse recordWeight(Long userId, WeightLogRequest req) {
        WeightLog result = weightLogRepository
                .findByUserIdAndMeasuredAt(userId, req.getMeasuredAt())
                .map(existing -> {
                    existing.update(req.getWeightKg(), req.getNote());
                    log.info("[WeightLog] Updated - userId={}, date={}, weight={}kg",
                            userId, req.getMeasuredAt(), req.getWeightKg());
                    return existing;
                })
                .orElseGet(() -> {
                    WeightLog newLog = WeightLog.builder()
                            .userId(userId)
                            .measuredAt(req.getMeasuredAt())
                            .weightKg(req.getWeightKg())
                            .note(req.getNote())
                            .build();
                    log.info("[WeightLog] Created - userId={}, date={}, weight={}kg",
                            userId, req.getMeasuredAt(), req.getWeightKg());
                    return weightLogRepository.save(newLog);
                });

        return WeightLogResponse.from(result);
    }

    /**
     * 월별 체중 목록 조회.
     * 리포트 페이지 캘린더 및 추이 차트에서 사용.
     */
    public List<WeightLogResponse> getMonthlyLogs(Long userId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        return weightLogRepository
                .findByUserIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(userId, from, to)
                .stream()
                .map(WeightLogResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 날짜 체중 삭제.
     * 본인 데이터 여부를 검증 후 삭제.
     */
    @Transactional
    public void deleteWeight(Long userId, LocalDate date) {
        weightLogRepository
                .findByUserIdAndMeasuredAt(userId, date)
                .ifPresent(existing -> {
                    if (!existing.getUserId().equals(userId)) {
                        throw new SecurityException("본인의 체중 데이터만 삭제할 수 있습니다.");
                    }
                    weightLogRepository.delete(existing);
                    log.info("[WeightLog] Deleted - userId={}, date={}", userId, date);
                });
    }
}