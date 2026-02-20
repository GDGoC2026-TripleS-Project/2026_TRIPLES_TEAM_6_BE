package com.lastcup.api.domain.menu.service;

import com.lastcup.api.domain.brand.domain.Brand;
import com.lastcup.api.domain.brand.repository.BrandRepository;
import com.lastcup.api.domain.menu.domain.*;
import com.lastcup.api.domain.menu.dto.response.MenuListItemResponse;
import com.lastcup.api.domain.menu.dto.response.MenuSearchResponse;
import com.lastcup.api.domain.menu.dto.response.MenuSizeDetailResponse;
import com.lastcup.api.domain.menu.dto.response.MenuSizeResponse;
import com.lastcup.api.domain.menu.dto.response.NutritionResponse;
import com.lastcup.api.domain.menu.mapper.MenuMapper;
import com.lastcup.api.domain.menu.repository.MenuRepository;
import com.lastcup.api.domain.menu.repository.MenuSizeRepository;
import com.lastcup.api.domain.menu.repository.MenuTemperatureRepository;
import com.lastcup.api.domain.user.repository.MenuFavoriteRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock private BrandRepository brandRepository;
    @Mock private MenuRepository menuRepository;
    @Mock private MenuFavoriteRepository menuFavoriteRepository;
    @Mock private MenuTemperatureRepository menuTemperatureRepository;
    @Mock private MenuSizeRepository menuSizeRepository;
    @Mock private MenuMapper menuMapper;

    @InjectMocks
    private MenuService menuService;

    @Test
    @DisplayName("findBrandMenus: 브랜드가 없으면 정확한 예외 메시지를 반환한다")
    void findBrandMenusThrowsWhenBrandMissing() {
        when(brandRepository.existsById(10L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> menuService.findBrandMenus(10L, null, null, null));

        assertEquals("Brand not found: 10", ex.getMessage());
    }

    @Test
    @DisplayName("findBrandMenus: category/keyword 4분기에서 올바른 repository 메서드를 호출한다")
    void findBrandMenusBranchCallsRepositoryMethod() {
        when(brandRepository.existsById(1L)).thenReturn(true);
        when(menuRepository.findByBrandIdAndIsActiveTrue(eq(1L), any(Sort.class))).thenReturn(List.of());
        when(menuRepository.findByBrandIdAndCategoryAndIsActiveTrue(eq(1L), eq(MenuCategory.COFFEE), any(Sort.class))).thenReturn(List.of());
        when(menuRepository.findByBrandIdAndNameContainingIgnoreCaseAndIsActiveTrue(eq(1L), eq("라떼"), any(Sort.class))).thenReturn(List.of());
        when(menuRepository.findByBrandIdAndCategoryAndNameContainingIgnoreCaseAndIsActiveTrue(eq(1L), eq(MenuCategory.COFFEE), eq("라떼"), any(Sort.class))).thenReturn(List.of());

        menuService.findBrandMenus(1L, null, null, null);
        menuService.findBrandMenus(1L, MenuCategory.COFFEE, null, null);
        menuService.findBrandMenus(1L, null, "라떼", null);
        menuService.findBrandMenus(1L, MenuCategory.COFFEE, "라떼", null);

        verify(menuRepository).findByBrandIdAndIsActiveTrue(eq(1L), any(Sort.class));
        verify(menuRepository).findByBrandIdAndCategoryAndIsActiveTrue(eq(1L), eq(MenuCategory.COFFEE), any(Sort.class));
        verify(menuRepository).findByBrandIdAndNameContainingIgnoreCaseAndIsActiveTrue(eq(1L), eq("라떼"), any(Sort.class));
        verify(menuRepository).findByBrandIdAndCategoryAndNameContainingIgnoreCaseAndIsActiveTrue(eq(1L), eq(MenuCategory.COFFEE), eq("라떼"), any(Sort.class));
    }

    @Test
    @DisplayName("findBrandMenus: userId가 있으면 favorite 우선, 내부는 name 오름차순 정렬된다")
    void findBrandMenusSortsFavoriteThenName() {
        when(brandRepository.existsById(1L)).thenReturn(true);

        Menu menu1 = mock(Menu.class); when(menu1.getId()).thenReturn(1L);
        Menu menu2 = mock(Menu.class); when(menu2.getId()).thenReturn(2L);
        Menu menu3 = mock(Menu.class); when(menu3.getId()).thenReturn(3L);
        when(menuRepository.findByBrandIdAndIsActiveTrue(eq(1L), any(Sort.class))).thenReturn(List.of(menu1, menu2, menu3));
        when(menuFavoriteRepository.findMenuIdsByUserId(99L)).thenReturn(List.of(2L));

        when(menuMapper.toMenuListItem(menu1, false)).thenReturn(new MenuListItemResponse(1L, "바", MenuCategory.COFFEE, null, false));
        when(menuMapper.toMenuListItem(menu2, true)).thenReturn(new MenuListItemResponse(2L, "다", MenuCategory.COFFEE, null, true));
        when(menuMapper.toMenuListItem(menu3, false)).thenReturn(new MenuListItemResponse(3L, "가", MenuCategory.COFFEE, null, false));

        List<MenuListItemResponse> result = menuService.findBrandMenus(1L, null, null, 99L);

        assertEquals(List.of(2L, 3L, 1L), result.stream().map(MenuListItemResponse::id).toList());
    }

    @Test
    @DisplayName("searchMenus: favorite 우선 후 brandName+name 정렬을 수행한다")
    void searchMenusSortsByFavoriteBrandName() {
        Menu m1 = mock(Menu.class); when(m1.getId()).thenReturn(1L);
        Menu m2 = mock(Menu.class); when(m2.getId()).thenReturn(2L);
        Menu m3 = mock(Menu.class); when(m3.getId()).thenReturn(3L);
        when(menuRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(eq("라"), any(Sort.class)))
                .thenReturn(List.of(m1, m2, m3));
        when(menuFavoriteRepository.findMenuIdsByUserId(5L)).thenReturn(List.of(3L));

        when(menuMapper.toMenuSearch(m1, false)).thenReturn(new MenuSearchResponse(1L, "브랜드B", "메뉴B", null, false));
        when(menuMapper.toMenuSearch(m2, false)).thenReturn(new MenuSearchResponse(2L, "브랜드A", "메뉴C", null, false));
        when(menuMapper.toMenuSearch(m3, true)).thenReturn(new MenuSearchResponse(3L, "브랜드Z", "메뉴A", null, true));

        List<MenuSearchResponse> result = menuService.searchMenus("라", 5L);

        assertEquals(List.of(3L, 2L, 1L), result.stream().map(MenuSearchResponse::id).toList());
    }

    @Test
    @DisplayName("findMenuSizeDetail: 없으면 정확한 예외 메시지를 반환한다")
    void findMenuSizeDetailThrowsWhenMissing() {
        when(menuSizeRepository.findDetailById(55L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> menuService.findMenuSizeDetail(55L));

        assertEquals("MenuSize not found: 55", ex.getMessage());
    }

    @Test
    @DisplayName("findMenuSizeDetail: DTO 내부 필드를 정확히 매핑한다")
    void findMenuSizeDetailMapsFields() {
        Brand brand = new Brand("브랜드", null);
        ReflectionTestUtils.setField(brand, "id", 7L);
        Menu menu = new Menu(brand, "아메리카노", MenuCategory.COFFEE);
        ReflectionTestUtils.setField(menu, "id", 11L);
        MenuTemperature mt = new MenuTemperature(menu, TemperatureType.HOT);
        Nutrition nutrition = mock(Nutrition.class);
        when(nutrition.getCaffeineMg()).thenReturn(120);
        when(nutrition.getSugarG()).thenReturn(1);

        MenuSize menuSize = mock(MenuSize.class);
        when(menuSizeRepository.findDetailById(100L)).thenReturn(Optional.of(menuSize));
        when(menuSize.getId()).thenReturn(100L);
        when(menuSize.getMenuTemperature()).thenReturn(mt);
        when(menuSize.getSizeName()).thenReturn("Tall");
        when(menuSize.getVolumeMl()).thenReturn(355);
        when(menuSize.getNutrition()).thenReturn(nutrition);
        when(menuMapper.toMenuSize(menuSize)).thenReturn(new MenuSizeResponse(100L, "Tall", 355,
                new NutritionResponse(120, 1, null, null, null, null)));

        MenuSizeDetailResponse response = menuService.findMenuSizeDetail(100L);

        assertEquals(100L, response.menuSizeId());
        assertEquals(11L, response.menuId());
        assertEquals("아메리카노", response.menuName());
        assertEquals("브랜드", response.brandName());
        assertEquals(TemperatureType.HOT, response.temperature());
        assertEquals("Tall", response.sizeName());
        assertEquals(120, response.nutrition().caffeineMg());
    }
}
