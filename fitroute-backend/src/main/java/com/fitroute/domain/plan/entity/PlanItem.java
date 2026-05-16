// src/main/java/com/fitroute/domain/plan/entity/PlanItem.java
package com.fitroute.domain.plan.entity;

import com.fitroute.global.enums.PlanItemCategory;
import com.fitroute.global.enums.PlanItemStatus;
import com.fitroute.global.enums.PlanItemType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan_items", indexes = {
        @Index(name = "idx_plan_items_plan_id", columnList = "plan_id"),
        @Index(name = "idx_plan_items_date", columnList = "date"),
        @Index(name = "idx_plan_items_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class PlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ★ Plan → DailyPlan 으로 수정 (plan_items.plan_id = daily_plans.id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private DailyPlan dailyPlan;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanItemType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanItemCategory category;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanItemStatus status = PlanItemStatus.PENDING;

    // ── MEAL 전용 ──────────────────────────────────
    private String foodName;

    // ── WORKOUT 전용 ───────────────────────────────
    private String exerciseName;
    private Integer sets;
    private Integer reps;
    private Integer weightKg; // 컬럼명 충돌 방지 (weight → weightKg)

    // ── 공통 ───────────────────────────────────────
    @Column(nullable = false)
    private Integer calories;

    private Integer protein;
    private Integer carbs;
    private Integer fat;
    private Integer durationMin; // WORKOUT 전용

    // ── 수정 내용 (원본 절대 덮어쓰지 않음) ────────────
    @Column(length = 200)
    private String modifiedName;
    private Integer modifiedCalories;
    private Integer modifiedSets;
    private Integer modifiedReps;
    private Integer modifiedProtein;
    private Integer modifiedCarbs;
    private Integer modifiedFat;

    private LocalDateTime statusUpdatedAt;

    // ── 유효 값 반환 (수정값 우선) ────────────────────
    public String getEffectiveName() {
        if (modifiedName != null)
            return modifiedName;
        return type == PlanItemType.MEAL ? foodName : exerciseName;
    }

    public int getEffectiveCalories() {
        return modifiedCalories != null ? modifiedCalories : calories;
    }

    public int getEffectiveSets() {
        if (modifiedSets != null)
            return modifiedSets;
        return sets != null ? sets : 0;
    }

    public int getEffectiveReps() {
        if (modifiedReps != null)
            return modifiedReps;
        return reps != null ? reps : 0;
    }

    // ── 도메인 메서드 ─────────────────────────────────
    public void modify(String name, Integer cal,
            Integer protein, Integer carbs, Integer fat,
            Integer sets, Integer reps) {
        this.status = PlanItemStatus.MODIFIED;
        this.statusUpdatedAt = LocalDateTime.now();
        if (name != null)
            this.modifiedName = name;
        if (cal != null)
            this.modifiedCalories = cal;
        if (protein != null)
            this.modifiedProtein = protein;
        if (carbs != null)
            this.modifiedCarbs = carbs;
        if (fat != null)
            this.modifiedFat = fat;
        if (sets != null)
            this.modifiedSets = sets;
        if (reps != null)
            this.modifiedReps = reps;
    }

    public void resetToPending() {
        this.status = PlanItemStatus.PENDING;
        this.statusUpdatedAt = null;
    }

    public void complete() {
        this.status = PlanItemStatus.COMPLETED;
        this.statusUpdatedAt = LocalDateTime.now();
        clearModifiedFields();
    }

    public void skip() {
        this.status = PlanItemStatus.SKIPPED;
        this.statusUpdatedAt = LocalDateTime.now();
        clearModifiedFields();
    }

    private void clearModifiedFields() {
        this.modifiedName = null;
        this.modifiedCalories = null;
        this.modifiedProtein = null;
        this.modifiedCarbs = null;
        this.modifiedFat = null;
        this.modifiedSets = null;
        this.modifiedReps = null;
    }

    public void edit(String name, Integer cal,
            Integer protein, Integer carbs, Integer fat,
            Integer sets, Integer reps) {
        this.status = PlanItemStatus.EDITED;
        this.statusUpdatedAt = LocalDateTime.now();
        if (name != null)
            this.modifiedName = name;
        if (cal != null)
            this.modifiedCalories = cal;
        if (protein != null)
            this.modifiedProtein = protein;
        if (carbs != null)
            this.modifiedCarbs = carbs;
        if (fat != null)
            this.modifiedFat = fat;
        if (sets != null)
            this.modifiedSets = sets;
        if (reps != null)
            this.modifiedReps = reps;
    }

    // ─── Setter 메서드 (PlanItem 생성 후 필드 설정용) ────
    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public void setSets(Integer sets) {
        this.sets = sets;
    }

    public void setReps(Integer reps) {
        this.reps = reps;
    }

    public void setWeightKg(Integer weightKg) {
        this.weightKg = weightKg;
    }

    public void setDurationMin(Integer durationMin) {
        this.durationMin = durationMin;
    }

    public void setProtein(Integer protein) {
        this.protein = protein;
    }

    public void setCarbs(Integer carbs) {
        this.carbs = carbs;
    }

    public void setFat(Integer fat) {
        this.fat = fat;
    }
}