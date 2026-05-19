// domain/user/repository/UserRepository.java
package com.fitroute.domain.user.repository;

import com.fitroute.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * GCM 암호화 특성상 encryptedEmail 컬럼으로 직접 조회 불가
 * 개선 완료: email_hash(SHA256) 단방향 해시 컬럼 기반으로 인덱스를 통한 O(1) 검색 수행
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // 해시값을 이용한 유저 정보 조회 (로그인 시 사용)
    Optional<User> findByEmailHash(String emailHash);

    // 해시값을 이용한 이메일 중복 체크 (회원가입 시 사용)
    boolean existsByEmailHash(String emailHash);
}