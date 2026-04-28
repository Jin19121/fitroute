// domain/user/entity/User.java
package com.fitroute.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email; // AES-256 암호화 저장

    @Column(nullable = false)
    private String password; // BCrypt 해시 저장

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProfile profile;

    public enum Status { ACTIVE, INACTIVE }

    @Builder
    public User(String email, String password) {
        this.email = email;
        this.password = password;
        this.status = Status.ACTIVE;
    }
}