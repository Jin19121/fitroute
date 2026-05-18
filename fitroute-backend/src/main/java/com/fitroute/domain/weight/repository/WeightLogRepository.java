// domain/weight/repository/WeightLogRepository.java
package com.fitroute.domain.weight.repository;

import com.fitroute.domain.weight.entity.WeightLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeightLogRepository extends JpaRepository<WeightLog, Long> {

        // upsert 판단용 — 날짜별 단건 조회
        Optional<WeightLog> findByUserIdAndMeasuredAt(Long userId, LocalDate measuredAt);

        // 월별 체중 목록 — 리포트 캘린더/차트용
        List<WeightLog> findByUserIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                        Long userId, LocalDate from, LocalDate to);

        // 사용자 최신 체중 한 건 — 대시보드 체중 카드용
        Optional<WeightLog> findFirstByUserIdOrderByMeasuredAtDesc(Long userId);

        // 기간 내 첫 번째 체중 — 월간 변화량 계산용 (시작점)
        @Query("""
                        SELECT wl FROM WeightLog wl
                        WHERE wl.userId = :userId
                          AND wl.measuredAt BETWEEN :from AND :to
                        ORDER BY wl.measuredAt ASC
                        LIMIT 1
                        """)
        Optional<WeightLog> findFirstInRange(
                                        @Param("userId") Long userId,
                        @Param("from") LocalDate from,
                        @Param("to") LocalDate to);
}