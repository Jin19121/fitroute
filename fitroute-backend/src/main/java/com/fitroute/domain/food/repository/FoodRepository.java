// domain/food/repository/FoodRepository.java
package com.fitroute.domain.food.repository;

import com.fitroute.domain.food.entity.Food;
import com.fitroute.global.enums.PlanItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Long> {

    // ─── AI 프롬프트용 "메뉴판" 조회 ──────────────────────────────
    List<Food> findByCategory(PlanItemCategory category);

    // ─── AI가 선택한 ID로 실제 데이터 조회 ────────────────────────
    List<Food> findByIdIn(List<Long> ids);

    // ─── 식단 스타일별 필터링 (tags 컬럼 활용) ─────────────────────
    // ex) dietStyle = "저지방" 이면 tags LIKE '%저지방%' 인 음식만 조회
    @Query("SELECT f FROM Food f WHERE f.category = :category AND f.tags LIKE %:tag%")
    List<Food> findByCategoryAndTag(
            @Param("category") PlanItemCategory category,
            @Param("tag") String tag);

    // ─── 중복 적재 방지 ────────────────────────────────────────────
    boolean existsByFoodCode(String foodCode);
}