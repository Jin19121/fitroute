// src/main/java/com/fitroute/domain/user/event/UserSignedUpEvent.java
package com.fitroute.domain.user.event;

/**
 * 회원가입 완료 후 비동기 처리를 위한 이벤트 객체
 * 
 * @param userId 가입된 사용자의 ID
 */
public record UserSignedUpEvent(Long userId) {
}