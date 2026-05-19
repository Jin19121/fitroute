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
@Table(name = "daily_plans", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "plan_date", "version" }))
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

    // ─── Versioning ─────────────────────────────────
    /**
     * 같은 날 플랜 계보의 최초 버전 id.
     * version=1 이면 @PostPersist가 자동으로 id와 동일하게 세팅.
     */
    @Column(name = "root_plan_id")
    private Long rootPlanId;

    /** 1부터 시작. 재생성마다 +1 */
    @Column(nullable = false)
    private int version = 1;
    // ────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────
    // Enum
    // ─────────────────────────────────────────────────
    public enum PlanStatus {
        PENDING, GENERATING, ACTIVE, COMPLETED, SKIPPED, FAILED,
        /** 재생성으로 교체된 이전 버전 */
        SUPERSEDED
    }

    // ─────────────────────────────────────────────────
    // Builder (version / rootPlanId 포함)
    // ─────────────────────────────────────────────────
    @Builder
    public DailyPlan(Long userId, LocalDate planDate, Integer version, Long rootPlanId) {
        this.userId = userId;
        this.planDate = planDate;
        this.status = PlanStatus.GENERATING;
        this.version = version != null ? version : 1;
        this.rootPlanId = rootPlanId; // null 이면 @PostPersist 에서 id로 초기화
    }

    // ─────────────────────────────────────────────────
    // JPA 콜백
    // ─────────────────────────────────────────────────

    /**
     * version=1 최초 저장 시 root_plan_id = id 자동 세팅.
     * Hibernate dirty-check 가 @PostPersist 이후 변경을 감지해 UPDATE 발행.
     */
    @PostPersist
    protected void initRootPlanId() {
        if (this.rootPlanId == null) {
            this.rootPlanId = this.id;
        }
    }

    // ─────────────────────────────────────────────────
    // 도메인 메서드
    // ─────────────────────────────────────────────────

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

    /**
     * 재생성 시 이전 버전을 SUPERSEDED 상태로 전환.
     */
    public void supersede() {
        this.status = PlanStatus.SUPERSEDED;
    }
}