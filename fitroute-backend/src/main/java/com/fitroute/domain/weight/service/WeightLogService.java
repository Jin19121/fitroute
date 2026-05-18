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
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeightLogService {

    private final WeightLogRepository weightLogRepository;

    @Transactional
    public WeightLogResponse recordWeight(Long userId, WeightLogRequest req) {
        WeightLog result = weightLogRepository
                .findByUserIdAndLogDate(userId, req.getLogDate())
                .map(existing -> {
                    existing.update(req.getWeightKg(), req.getBodyFatPct(),
                            req.getMuscleMass(), req.getNote());
                    log.info("[WeightLog] Updated - userId={}, date={}, weight={}kg",
                            userId, req.getLogDate(), req.getWeightKg());
                    return existing;
                })
                .orElseGet(() -> {
                    WeightLog newLog = WeightLog.builder()
                            .userId(userId)
                            .logDate(req.getLogDate())
                            .weightKg(req.getWeightKg())
                            .bodyFatPct(req.getBodyFatPct())
                            .muscleMass(req.getMuscleMass())
                            .note(req.getNote())
                            .build();
                    log.info("[WeightLog] Created - userId={}, date={}, weight={}kg",
                            userId, req.getLogDate(), req.getWeightKg());
                    return weightLogRepository.save(newLog);
                });

        return WeightLogResponse.from(result);
    }

    /** 오늘 체중 조회 */
    public Optional<WeightLogResponse> getTodayWeight(Long userId) {
        return weightLogRepository
                .findByUserIdAndLogDate(userId, LocalDate.now())
                .map(WeightLogResponse::from);
    }

    /** 가장 최근 체중 1건 조회 */
    public Optional<WeightLogResponse> getLatestWeight(Long userId) {
        return weightLogRepository
                .findFirstByUserIdOrderByLogDateDesc(userId)
                .map(WeightLogResponse::from);
    }

    /** 월별 체중 목록 조회 */
    public List<WeightLogResponse> getMonthlyLogs(Long userId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        return weightLogRepository
                .findByUserIdAndLogDateBetweenOrderByLogDateAsc(userId, from, to)
                .stream()
                .map(WeightLogResponse::from)
                .collect(Collectors.toList());
    }

    /** 특정 날짜 체중 삭제 */
    @Transactional
    public void deleteWeight(Long userId, LocalDate date) {
        weightLogRepository
                .findByUserIdAndLogDate(userId, date)
                .ifPresent(existing -> {
                    if (!existing.getUserId().equals(userId)) {
                        throw new SecurityException("본인의 체중 데이터만 삭제할 수 있습니다.");
                    }
                    weightLogRepository.delete(existing);
                    log.info("[WeightLog] Deleted - userId={}, date={}", userId, date);
                });
    }
}