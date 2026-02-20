package com.lastcup.api.domain.menu.repository;

import com.lastcup.api.domain.brand.domain.Brand;
import com.lastcup.api.domain.menu.domain.Menu;
import com.lastcup.api.domain.menu.domain.MenuCategory;
import jakarta.persistence.EntityManager;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.data.domain.Sort;

@DataJpaTest
@ActiveProfiles("test")
class MenuRepositoryTest {

    @Autowired private MenuRepository menuRepository;
    @Autowired private EntityManager em;

    @Test
    @DisplayName("findWithBrandById: 조회 후 brand 접근 시 LazyInitializationException이 발생하지 않는다")
    void findWithBrandByIdFetchesBrand() {
        Brand brand = new Brand("브랜드", null);
        em.persist(brand);
        Menu menu = new Menu(brand, "아메리카노", MenuCategory.COFFEE);
        em.persist(menu);
        em.flush();
        em.clear();

        Menu found = menuRepository.findWithBrandById(menu.getId()).orElseThrow();

        assertDoesNotThrow(() -> found.getBrand().getName());
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCaseAndIsActiveTrue: inactive 메뉴는 조회되지 않는다")
    void findByNameContainingIgnoreCaseAndIsActiveTrueExcludesInactive() {
        Brand brand = new Brand("브랜드", null);
        em.persist(brand);

        Menu active = new Menu(brand, "카페라떼", MenuCategory.COFFEE);
        Menu inactive = new Menu(brand, "카페라떼 디카페인", MenuCategory.COFFEE);
        ReflectionTestUtils.setField(inactive, "isActive", false);

        em.persist(active);
        em.persist(inactive);
        em.flush();
        em.clear();

        List<Menu> result = menuRepository.findByNameContainingIgnoreCaseAndIsActiveTrue("카페", Sort.by("name"));

        assertEquals(1, result.size());
        assertEquals(active.getName(), result.get(0).getName());
    }
}
