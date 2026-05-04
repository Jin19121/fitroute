package com.fitroute.domain.plan.entity;

import com.fitroute.global.enums.PlanItemCategory;
import com.fitroute.global.enums.PlanItemType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "plan_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class PlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanItemType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanItemCategory category;

    // MEAL 전용
    private String foodName;

    // WORKOUT 전용
    private String exerciseName;
    private Integer sets;
    private Integer reps;
    private Integer weight;

    // 공통
    @Column(nullable = false)
    private Integer calories;

    private Integer protein;
    private Integer carbs;
    private Integer fat;
}