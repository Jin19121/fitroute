// domain/user/entity/UserProfile.java
package com.fitroute.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Float height;
    private Float weight;
    private Float targetWeight;
    private Integer targetPeriod; // 단위: 주(week)

    @Builder
    public UserProfile(User user, Float height, Float weight,
                       Float targetWeight, Integer targetPeriod) {
        this.user = user;
        this.height = height;
        this.weight = weight;
        this.targetWeight = targetWeight;
        this.targetPeriod = targetPeriod;
    }
}