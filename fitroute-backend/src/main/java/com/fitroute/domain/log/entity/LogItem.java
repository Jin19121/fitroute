// domain/log/entity/LogItem.java
package com.fitroute.domain.log.entity;

import com.fitroute.domain.plan.entity.PlanItem;
import com.fitroute.global.enums.PlanItemStatus;
import com.fitroute.global.enums.PlanItemType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "log_items", indexes = {
        @Index(name = "idx_log_items_log_id", columnList = "log_id"),
        @Index(name = "idx_log_items_plan_item_id", columnList = "plan_item_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class LogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    private Log log;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_item_id", nullable = false)
    private PlanItem planItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanItemType type;

    // ─── 이름 ────────────────────────────────────────
    @Column(name = "original_name", length = 200)
    private String originalName;

    @Column(name = "actual_name", length = 200)
    private String actualName;

    // ─── 칼로리 ──────────────────────────────────────
    @Column(name = "original_calories")
    private Integer originalCalories;

    @Column(name = "actual_calories")
    private Integer actualCalories;

    /** actual - original (음수 = 절감, 양수 = 초과) */
    @Column(name = "diff_calories")
    private Integer diffCalories;

    // ─── MEAL 전용 영양소 ─────────────────────────────
    @Column(name = "original_protein")
    private Integer originalProtein;

    @Column(name = "actual_protein")
    private Integer actualProtein;

    @Column(name = "original_carbs")
    private Integer originalCarbs;

    @Column(name = "actual_carbs")
    private Integer actualCarbs;

    @Column(name = "original_fat")
    private Integer originalFat;

    @Column(name = "actual_fat")
    private Integer actualFat;

    // ─── 상태 ────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanItemStatus status; // COMPLETED | SKIPPED

    @Column(name = "is_modified", nullable = false)
    @Builder.Default
    private boolean isModified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ─── 도메인 메서드 ───────────────────────────────
    public void update(
            PlanItemType type,
            String originalName, String actualName,
            int originalCalories, int actualCalories,
            Integer originalProtein, Integer actualProtein,
            Integer originalCarbs, Integer actualCarbs,
            Integer originalFat, Integer actualFat,
            PlanItemStatus status,
            boolean isModified) {

        this.type = type;
        this.originalName = originalName;
        this.actualName = actualName;
        this.originalCalories = originalCalories;
        this.actualCalories = actualCalories;
        this.diffCalories = actualCalories - originalCalories;
        this.originalProtein = originalProtein;
        this.actualProtein = actualProtein;
        this.originalCarbs = originalCarbs;
        this.actualCarbs = actualCarbs;
        this.originalFat = originalFat;
        this.actualFat = actualFat;
        this.status = status;
        this.isModified = isModified;
    }
}