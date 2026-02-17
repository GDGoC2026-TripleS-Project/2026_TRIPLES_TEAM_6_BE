package com.lastcup.api.domain.menu.repository;

import com.lastcup.api.domain.menu.domain.Menu;
import com.lastcup.api.domain.menu.domain.MenuCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByBrandIdAndIsActiveTrue(Long brandId, Sort sort);

    List<Menu> findByBrandIdAndCategoryAndIsActiveTrue(Long brandId, MenuCategory category, Sort sort);

    List<Menu> findByBrandIdAndNameContainingIgnoreCaseAndIsActiveTrue(Long brandId, String keyword, Sort sort);

    List<Menu> findByBrandIdAndCategoryAndNameContainingIgnoreCaseAndIsActiveTrue(
            Long brandId,
            MenuCategory category,
            String keyword,
            Sort sort
    );

    @EntityGraph(attributePaths = {"brand"})
    List<Menu> findByNameContainingIgnoreCaseAndIsActiveTrue(String keyword, Sort sort);

    @EntityGraph(attributePaths = {"brand"})
    Optional<Menu> findWithBrandById(Long id);

    @Query("select m from Menu m join fetch m.brand where m.id in :menuIds and m.isActive = true")
    List<Menu> findByIdInWithBrandAndIsActiveTrue(@Param("menuIds") List<Long> menuIds);
}
