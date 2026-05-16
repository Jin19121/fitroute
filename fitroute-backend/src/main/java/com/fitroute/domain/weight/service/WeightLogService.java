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
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeightLogService {

    private final WeightLogRepository weightLogRepository;

    /**
     * 오늘 체중 기록 — 이미 있으면 업데이트, 없으면 신규 생성 (upsert)
     */
    @Transactional
    public WeightLogResponse logTodayWeight(Long userId, WeightLogRequest req) {
        LocalDate today = LocalDate.now();

        WeightLog weightLog = weightLogRepository // ← log → weightLog로 변경
                .findByUserIdAndLogDate(userId, today)
                .map(existing -> {
                    existing.update(req.getWeight(), req.getBodyFatPct(), req.getMuscleMass());
                    return existing;
                })
                .orElseGet(() -> weightLogRepository.save(
                        WeightLog.builder()
                                .userId(userId)
                                .logDate(today)
                                .weight(req.getWeight())
                                .bodyFatPct(req.getBodyFatPct())
                                .muscleMass(req.getMuscleMass())
                                .build()));

        Float change = calculateChange(userId, today, weightLog.getWeight());

        log.info("[WeightLog] Saved - userId={}, date={}, weight={}", // ← 이제 SLF4J log
                userId, today, weightLog.getWeight());

        return WeightLogResponse.from(weightLog, change);
    }

    /**
     * 오늘 체중 조회 (없으면 null)
     */
    public Optional<WeightLogResponse> getTodayWeight(Long userId) {
        return weightLogRepository
                .findByUserIdAndLogDate(userId, LocalDate.now())
                .map(log -> {
                    Float change = calculateChange(userId, LocalDate.now(), log.getWeight());
                    return WeightLogResponse.from(log, change);
                });
    }

    private Float calculateChange(Long userId, LocalDate date, Float currentWeight) {
        return weightLogRepository
                .findPreviousLog(userId, date)
                .map(prev -> currentWeight - prev.getWeight())
                .orElse(null);
    }
}