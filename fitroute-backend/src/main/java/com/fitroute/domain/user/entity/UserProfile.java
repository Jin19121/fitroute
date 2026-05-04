// domain/user/entity/UserProfile.java
package com.fitroute.domain.user.entity;

import com.fitroute.global.enums.ActivityLevel;
import com.fitroute.global.enums.DietStyle;
import com.fitroute.global.enums.ExerciseExperience;
import com.fitroute.global.enums.Gender;
import com.fitroute.global.enums.GoalType;
import jakarta.persistence.*;
import lombok.*;
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

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityLevel activityLevel;

    // 추가된 필드
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalType goalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExerciseExperience exerciseExperience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DietStyle dietStyle;

    @Builder
    public UserProfile(User user, Float height, Float weight,
            Float targetWeight, Integer targetPeriod,
            Gender gender, LocalDate birthDate,
            ActivityLevel activityLevel,
            GoalType goalType,
            ExerciseExperience exerciseExperience,
            DietStyle dietStyle) {
        this.user = user;
        this.height = height;
        this.weight = weight;
        this.targetWeight = targetWeight;
        this.targetPeriod = targetPeriod;
        this.gender = gender;
        this.birthDate = birthDate;
        this.activityLevel = activityLevel;
        this.goalType = goalType;
        this.exerciseExperience = exerciseExperience;
        this.dietStyle = dietStyle;
    }
}