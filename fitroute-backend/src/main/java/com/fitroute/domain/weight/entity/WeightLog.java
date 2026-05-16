// domain/weight/entity/WeightLog.java
package com.fitroute.domain.weight.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weight_logs", indexes = {
        @Index(name = "idx_weight_logs_user_date", columnList = "user_id, log_date")
}, uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "log_date" }))
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

    @Column(nullable = false)
    private Float weight; // kg

    @Column(name = "body_fat_pct")
    private Float bodyFatPct; // 체지방률 (선택)

    @Column(name = "muscle_mass")
    private Float muscleMass; // 근육량 kg (선택)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void update(Float weight, Float bodyFatPct, Float muscleMass) {
        if (weight != null)
            this.weight = weight;
        if (bodyFatPct != null)
            this.bodyFatPct = bodyFatPct;
        if (muscleMass != null)
            this.muscleMass = muscleMass;
    }
}