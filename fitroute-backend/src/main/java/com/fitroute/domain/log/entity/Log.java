// domain/log/entity/Log.java
package com.fitroute.domain.log.entity;

import com.fitroute.domain.plan.entity.DailyPlan;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "logs", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id",
        "log_date" }), indexes = @Index(name = "idx_logs_user_date", columnList = "user_id, log_date"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_plan_id", nullable = false)
    private DailyPlan dailyPlan;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "planned_calories")
    private Integer plannedCalories;

    @Column(name = "consumed_calories", nullable = false)
    @Builder.Default
    private int consumedCalories = 0;

    @Column(name = "burned_calories", nullable = false)
    @Builder.Default
    private int burnedCalories = 0;

    @Column(name = "completion_rate")
    @Builder.Default
    private Float completionRate = 0f;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── 팩토리 메서드 ────────────────────────────────
    public static Log create(Long userId, DailyPlan dailyPlan, LocalDate logDate) {
        return Log.builder()
                .userId(userId)
                .dailyPlan(dailyPlan)
                .logDate(logDate)
                .plannedCalories(dailyPlan.getCalorieTarget())
                .build();
    }

    // ─── 도메인 메서드 ───────────────────────────────
    public void updateAggregates(int consumedCalories, int burnedCalories, float completionRate) {
        this.consumedCalories = consumedCalories;
        this.burnedCalories = burnedCalories;
        this.completionRate = completionRate;
    }

    /**
     * 플랜 재생성 시 새 플랜으로 연결 갱신 + 집계 초기화
     */
    public void resetForNewPlan(DailyPlan newPlan) {
        this.dailyPlan = newPlan;
        this.plannedCalories = newPlan.getCalorieTarget();
        this.consumedCalories = 0;
        this.burnedCalories = 0;
        this.completionRate = 0f;
    }
}