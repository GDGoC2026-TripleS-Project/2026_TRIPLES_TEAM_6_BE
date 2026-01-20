package com.lastcup.api.domain.menu.repository;

import com.lastcup.api.domain.menu.domain.Menu;
import com.lastcup.api.domain.menu.domain.MenuCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    Page<Menu> findByBrandIdAndIsActiveTrue(Long brandId, Pageable pageable);

    Page<Menu> findByBrandIdAndCategoryAndIsActiveTrue(Long brandId, MenuCategory category, Pageable pageable);

    Page<Menu> findByBrandIdAndNameContainingIgnoreCaseAndIsActiveTrue(Long brandId, String keyword, Pageable pageable);

    Page<Menu> findByBrandIdAndCategoryAndNameContainingIgnoreCaseAndIsActiveTrue(
            Long brandId,
            MenuCategory category,
            String keyword,
            Pageable pageable
    );

    Page<Menu> findByNameContainingIgnoreCaseAndIsActiveTrue(String keyword, Pageable pageable);
}
