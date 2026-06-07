// global/event/DashboardCacheEvictListener.java
package com.fitroute.global.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.fitroute.global.cache.CacheKeyConstants;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardCacheEvictListener {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * DB 커밋 완료 시점에 캐시 무효화 실행
     *
     * @TransactionalEventListener(AFTER_COMMIT) 사용 이유:
     *                                           applyItemAction()의 @Transactional이
     *                                           커밋되기 전에 캐시를 지우면
     *                                           다른 요청이 구 데이터를 DB에서 읽어 다시 캐싱하는 Race
     *                                           Condition이 발생함.
     *                                           커밋 완료 후 삭제함으로써 항상 최신 데이터가 캐싱되도록 보장.
     *
     * @Async 사용 이유:
     *        캐시 무효화는 응답 반환과 무관한 부가 작업이므로 별도 스레드에서 처리.
     *        Redis 장애가 발생해도 API 응답에 영향을 주지 않음.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDashboardCacheEvict(DashboardCacheEvictEvent event) {
        String cacheKey = CacheKeyConstants.TODAY_CACHE_PREFIX + event.userId();
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("[DashboardCache] 캐시 무효화 성공 - key={}", cacheKey);
            } else {
                log.debug("[DashboardCache] 삭제할 캐시 없음 (이미 만료) - key={}", cacheKey);
            }
        } catch (Exception e) {
            // Redis 장애 시 로그만 남기고 무시 — 다음 조회 시 DB에서 새로 적재됨
            log.error("[DashboardCache] 캐시 무효화 실패 - key={}, error={}", cacheKey, e.getMessage());
        }
    }
}