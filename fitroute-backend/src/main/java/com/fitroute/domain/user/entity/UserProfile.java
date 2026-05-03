// domain/user/entity/UserProfile.java
package com.fitroute.domain.user.entity;

import com.fitroute.global.enums.ActivityLevel;
import com.fitroute.global.enums.Gender;
import org.hibernate.annotations.ColumnDefault;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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

    @Column(nullable = false)
    private Float height;

    @Column(nullable = false)
    private Float weight;

    @Column(nullable = false)
    private Float targetWeight;

    @Column(nullable = false)
    private Integer targetPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(name = "birth_date")
    @ColumnDefault("'1900-01-01'")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityLevel activityLevel;

    @Builder
    public UserProfile(User user, Float height, Float weight,
            Float targetWeight, Integer targetPeriod,
            Gender gender, LocalDate birthDate,
            ActivityLevel activityLevel) {
        this.user = user;
        this.height = height;
        this.weight = weight;
        this.targetWeight = targetWeight;
        this.targetPeriod = targetPeriod;
        this.gender = gender;
        this.birthDate = birthDate;
        this.activityLevel = activityLevel;
    }
}