// domain/weight/entity/WeightLog.java
package com.fitroute.domain.weight.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weight_logs", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id",
        "log_date" }), indexes = @Index(name = "idx_weight_user_date", columnList = "user_id, log_date"))
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

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "weight_kg", nullable = false)
    private Float weightKg;

    @Column(name = "body_fat_pct")
    private Float bodyFatPct;

    @Column(name = "muscle_mass")
    private Float muscleMass;

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

    // ★ 수정: bodyFatPct, muscleMass 포함
    public void update(Float weightKg, Float bodyFatPct, Float muscleMass, String note) {
        if (weightKg != null)
            this.weightKg = weightKg;
        if (bodyFatPct != null)
            this.bodyFatPct = bodyFatPct;
        if (muscleMass != null)
            this.muscleMass = muscleMass;
        if (note != null)
            this.note = note;
    }
}