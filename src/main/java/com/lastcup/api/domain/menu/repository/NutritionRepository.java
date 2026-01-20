package com.lastcup.api.domain.menu.repository;

import com.lastcup.api.domain.menu.domain.Nutrition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutritionRepository extends JpaRepository<Nutrition, Long> {
}
