// domain/log/repository/LogRepository.java
package com.fitroute.domain.log.repository;

import com.fitroute.domain.log.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LogRepository extends JpaRepository<Log, Long> {

    Optional<Log> findByUserIdAndLogDate(Long userId, LocalDate logDate);

    List<Log> findByUserIdAndLogDateBetweenOrderByLogDateAsc(
            Long userId, LocalDate from, LocalDate to);
}