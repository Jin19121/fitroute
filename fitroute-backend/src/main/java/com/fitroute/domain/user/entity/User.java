// domain/user/entity/User.java
package com.fitroute.domain.user.entity;

import com.fitroute.global.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "encrypted_email", nullable = false, unique = true)
    private String encryptedEmail;

    // 검색 성능 최적화를 위한 SHA-256 단방향 해시 컬럼 추가
    @Column(name = "email_hash", nullable = false, unique = true)
    private String emailHash;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProfile profile;

    @Builder
    public User(String encryptedEmail, String emailHash, String password, UserRole role) {
        this.encryptedEmail = encryptedEmail;
        this.emailHash = emailHash;
        this.password = password;
        this.role = role;
    }
}