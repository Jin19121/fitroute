// domain/log/repository/LogItemRepository.java
package com.fitroute.domain.log.repository;

import com.fitroute.domain.log.entity.Log;
import com.fitroute.domain.log.entity.LogItem;
import com.fitroute.domain.plan.entity.PlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LogItemRepository extends JpaRepository<LogItem, Long> {

    Optional<LogItem> findByLogAndPlanItem(Log log, PlanItem planItem);

    List<LogItem> findByLog(Log log);

    void deleteByLog(Log log);
}