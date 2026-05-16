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

    Optional<WeightLog> findByUserIdAndLogDate(Long userId, LocalDate logDate);

    // 최근 N일 조회 (리포트용)
    List<WeightLog> findByUserIdAndLogDateBetweenOrderByLogDateAsc(
            Long userId, LocalDate from, LocalDate to);

    // 직전 기록 (변화량 계산용)
    @Query("""
            SELECT w FROM WeightLog w
            WHERE w.userId = :userId
              AND w.logDate < :date
            ORDER BY w.logDate DESC
            LIMIT 1
            """)
    Optional<WeightLog> findPreviousLog(
            @Param("userId") Long userId,
            @Param("date") LocalDate date);

    // 최신 기록 1건
    Optional<WeightLog> findFirstByUserIdOrderByLogDateDesc(Long userId);
}