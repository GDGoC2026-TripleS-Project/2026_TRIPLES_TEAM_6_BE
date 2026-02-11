package com.lastcup.api.domain.option.repository;

import com.lastcup.api.domain.option.domain.Option;
import com.lastcup.api.domain.option.domain.OptionCategory;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OptionRepository extends JpaRepository<Option, Long> {

    long countByIdIn(Collection<Long> ids);

    @Query("SELECT o FROM Option o LEFT JOIN FETCH o.nutrition WHERE o.id IN :ids")
    List<Option> findAllWithNutritionByIdIn(@Param("ids") Collection<Long> ids);

    /**
     * 브랜드의 전체 옵션을 영양 정보와 함께 조회한다.
     * OptionResponse에서 옵션별 카페인 정보를 반환하기 위해 nutrition을 페치 조인한다.
     */
    @Query("SELECT o FROM Option o LEFT JOIN FETCH o.nutrition WHERE o.brandId = :brandId")
    List<Option> findAllWithNutritionByBrandId(@Param("brandId") Long brandId);

    /**
     * 브랜드의 특정 카테고리 옵션을 영양 정보와 함께 조회한다.
     */
    @Query("SELECT o FROM Option o LEFT JOIN FETCH o.nutrition WHERE o.brandId = :brandId AND o.category = :category")
    List<Option> findAllWithNutritionByBrandIdAndCategory(
            @Param("brandId") Long brandId,
            @Param("category") OptionCategory category
    );
}
