// domain/food/entity/Food.java
package com.fitroute.domain.food.entity;

import com.fitroute.global.enums.PlanItemCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "foods", indexes = {
        @Index(name = "idx_foods_category", columnList = "category"),
        @Index(name = "idx_foods_name", columnList = "name")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── 식약처 원본 식별자 (중복 적재 방지용) ──────────────
    @Column(name = "food_code", unique = true, length = 50)
    private String foodCode; // 식약처 FOOD_CD 필드

    // ─── 음식 기본 정보 ────────────────────────────────────
    @Column(nullable = false, length = 100)
    private String name; // 음식명 (FOOD_NM)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanItemCategory category; // BREAKFAST, LUNCH, DINNER, SNACK

    @Column(name = "serving_size", length = 50)
    private String servingSize; // 1회 제공량 (SERVING_SIZE)

    // ─── 영양 성분 (식약처 기준 100g당 or 1회 제공량 기준) ──
    @Column(nullable = false)
    private Integer calories; // 에너지(kcal) - NUTR_CONT1

    @Column(nullable = false)
    private Integer protein; // 단백질(g) - NUTR_CONT2

    @Column(nullable = false)
    private Integer fat; // 지방(g) - NUTR_CONT3

    @Column(nullable = false)
    private Integer carbs; // 탄수화물(g) - NUTR_CONT4

    // ─── 검색/필터링용 태그 ────────────────────────────────
    @Column(length = 200)
    private String tags; // "고단백,저지방,한식" — AI 프롬프트에서 필터링 힌트로 사용

    // ─── 데이터 출처 ───────────────────────────────────────
    @Column(name = "data_source", length = 50)
    @Builder.Default
    private String dataSource = "MFDS"; // Ministry of Food and Drug Safety (식약처)
}