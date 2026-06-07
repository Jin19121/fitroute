// global/event/DashboardCacheEvictEvent.java
package com.fitroute.global.event;

/**
 * DB 트랜잭션 커밋 완료 후 대시보드 캐시를 무효화하기 위한 이벤트
 * DashboardService.applyItemAction() 에서 발행
 * DashboardCacheEvictListener 에서 AFTER_COMMIT 시점에 수신
 */
public record DashboardCacheEvictEvent(Long userId) {
}