package com.lastcup.api.domain.menu.repository;

import com.lastcup.api.domain.brand.domain.Brand;
import com.lastcup.api.domain.menu.domain.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class MenuSizeRepositoryTest {

    @Autowired private MenuSizeRepository menuSizeRepository;
    @Autowired private EntityManager em;

    @Test
    @DisplayName("findDetailById: 메뉴/브랜드/영양 정보가 함께 조회된다")
    void findDetailByIdFetchesAssociations() throws Exception {
        Brand brand = new Brand("브랜드", null);
        em.persist(brand);
        Menu menu = new Menu(brand, "아메리카노", MenuCategory.COFFEE);
        em.persist(menu);
        MenuTemperature mt = new MenuTemperature(menu, TemperatureType.HOT);
        em.persist(mt);

        Nutrition nutrition = newInstance(Nutrition.class);
        ReflectionTestUtils.setField(nutrition, "caffeineMg", 100);
        em.persist(nutrition);

        MenuSize menuSize = newInstance(MenuSize.class);
        ReflectionTestUtils.setField(menuSize, "menuTemperature", mt);
        ReflectionTestUtils.setField(menuSize, "nutrition", nutrition);
        ReflectionTestUtils.setField(menuSize, "sizeName", "Tall");
        ReflectionTestUtils.setField(menuSize, "volumeMl", 355);
        em.persist(menuSize);
        em.flush();
        em.clear();

        MenuSize found = menuSizeRepository.findDetailById(menuSize.getId()).orElseThrow();

        assertEquals("브랜드", found.getMenuTemperature().getMenu().getBrand().getName());
        assertEquals(100, found.getNutrition().getCaffeineMg());
    }

    private <T> T newInstance(Class<T> type) throws Exception {
        Constructor<T> ctor = type.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }
}
