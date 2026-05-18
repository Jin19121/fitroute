// domain/weight/entity/WeightLog.java
package com.fitroute.domain.weight.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weight_logs", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id",
        "measured_at" }), indexes = @Index(name = "idx_weight_user_date", columnList = "user_id, measured_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class WeightLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "measured_at", nullable = false)
    private LocalDate measuredAt;

    @Column(name = "weight_kg", nullable = false)
    private Float weightKg;

    @Column(length = 200)
    private String note;

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

    // 같은 날짜 재측정 시 upsert용 도메인 메서드
    public void update(Float weightKg, String note) {
        this.weightKg = weightKg;
        this.note = note;
    }
}