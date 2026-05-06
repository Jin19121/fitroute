// domain/plan/entity/DailyPlan.java
package com.fitroute.domain.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "daily_plans", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "plan_date" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status = PlanStatus.PENDING;

    @Column(name = "calorie_target")
    private Integer calorieTarget;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meal_plan", columnDefinition = "json")
    private Map<String, Object> mealPlan;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "workout_plan", columnDefinition = "json")
    private Map<String, Object> workoutPlan;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_meta", columnDefinition = "json")
    private Map<String, Object> aiMeta;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum PlanStatus {
        PENDING, GENERATING, ACTIVE, COMPLETED, SKIPPED, FAILED // GENERATING, FAILED 추가
    }

    @Builder
    public DailyPlan(Long userId, LocalDate planDate) {
        this.userId = userId;
        this.planDate = planDate;
        this.status = PlanStatus.GENERATING; // 생성 시작 시점에 GENERATING
    }

    // 상태 전환 메서드
    public void complete(Integer calorieTarget,
            Map<String, Object> mealPlan,
            Map<String, Object> workoutPlan,
            Map<String, Object> aiMeta) {
        this.status = PlanStatus.ACTIVE;
        this.calorieTarget = calorieTarget;
        this.mealPlan = mealPlan;
        this.workoutPlan = workoutPlan;
        this.aiMeta = aiMeta;
    }

    public void fail() {
        this.status = PlanStatus.FAILED;
    }
}