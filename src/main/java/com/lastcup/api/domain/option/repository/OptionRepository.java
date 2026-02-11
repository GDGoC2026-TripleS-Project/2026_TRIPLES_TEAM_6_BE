package com.lastcup.api.domain.option.repository;

import com.lastcup.api.domain.option.domain.Option;
import com.lastcup.api.domain.option.domain.OptionCategory;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OptionRepository extends JpaRepository<Option, Long> {

    List<Option> findByBrandId(Long brandId);

    List<Option> findByBrandIdAndCategory(Long brandId, OptionCategory category);

    long countByIdIn(Collection<Long> ids);

    @Query("SELECT o FROM Option o LEFT JOIN FETCH o.nutrition WHERE o.id IN :ids")
    List<Option> findAllWithNutritionByIdIn(@Param("ids") Collection<Long> ids);
}
