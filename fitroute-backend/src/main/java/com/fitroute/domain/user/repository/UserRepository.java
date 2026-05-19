// domain/user/repository/UserRepository.java
package com.fitroute.domain.user.repository;

import com.fitroute.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * GCM 암호화 특성상 encryptedEmail 컬럼으로 직접 조회 불가
 * (동일 평문이라도 IV가 달라 암호문이 매번 다름)
 *
 * 현재: findAll() 후 AuthService에서 복호화 비교 (MVP 단계)
 * 개선 예정: email_hash(SHA256) 컬럼 추가 → 해시 기반 O(1) 조회
 */
public interface UserRepository extends JpaRepository<User, Long> {
    // findByEncryptedEmail, existsByEncryptedEmail 제거
    // AuthService에서 findAll() + decrypt() 로 대체
}