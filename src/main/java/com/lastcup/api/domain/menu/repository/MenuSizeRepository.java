package com.lastcup.api.domain.menu.repository;

import com.lastcup.api.domain.menu.domain.MenuSize;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MenuSizeRepository extends JpaRepository<MenuSize, Long> {

    @EntityGraph(attributePaths = {"nutrition"})
    List<MenuSize> findByMenuTemperatureIdOrderByIdAsc(Long menuTemperatureId);

    @Query("""
            select ms
            from MenuSize ms
            join fetch ms.nutrition n
            join fetch ms.menuTemperature mt
            join fetch mt.menu m
            join fetch m.brand b
            where ms.id = :menuSizeId
            """)
    Optional<MenuSize> findDetailById(Long menuSizeId);
}
