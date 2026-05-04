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

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    // 이 부분이 추가되어야 user.getProfile()이 작동합니다.
    // mappedBy는 UserProfile 엔티티에 정의된 User 필드명("user")과 일치해야 합니다.
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProfile profile;

    @Builder
    public User(String encryptedEmail, String password, UserRole role) {
        this.encryptedEmail = encryptedEmail;
        this.password = password;
        this.role = role;
    }
}