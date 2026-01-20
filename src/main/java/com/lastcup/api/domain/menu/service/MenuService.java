package com.lastcup.api.domain.menu.service;

import com.lastcup.api.domain.brand.domain.Brand;
import com.lastcup.api.domain.menu.domain.Menu;
import com.lastcup.api.domain.menu.domain.MenuCategory;
import com.lastcup.api.domain.menu.domain.MenuSize;
import com.lastcup.api.domain.menu.domain.MenuTemperature;
import com.lastcup.api.domain.menu.domain.TemperatureType;
import com.lastcup.api.domain.menu.dto.response.MenuDetailResponse;
import com.lastcup.api.domain.menu.dto.response.MenuListItemResponse;
import com.lastcup.api.domain.menu.dto.response.MenuSearchResponse;
import com.lastcup.api.domain.menu.dto.response.MenuSizeDetailResponse;
import com.lastcup.api.domain.menu.dto.response.MenuSizeResponse;
import com.lastcup.api.domain.menu.dto.response.NutritionResponse;
import com.lastcup.api.domain.menu.dto.response.PageResponse;
import com.lastcup.api.domain.brand.repository.BrandRepository;
import com.lastcup.api.domain.menu.repository.MenuRepository;
import com.lastcup.api.domain.menu.repository.MenuSizeRepository;
import com.lastcup.api.domain.menu.repository.MenuTemperatureRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MenuService {

    private final BrandRepository brandRepository;
    private final MenuRepository menuRepository;
    private final MenuTemperatureRepository menuTemperatureRepository;
    private final MenuSizeRepository menuSizeRepository;

    public MenuService(
            BrandRepository brandRepository,
            MenuRepository menuRepository,
            MenuTemperatureRepository menuTemperatureRepository,
            MenuSizeRepository menuSizeRepository
    ) {
        this.brandRepository = brandRepository;
        this.menuRepository = menuRepository;
        this.menuTemperatureRepository = menuTemperatureRepository;
        this.menuSizeRepository = menuSizeRepository;
    }

    public PageResponse<MenuListItemResponse> findBrandMenus(
            Long brandId,
            MenuCategory category,
            String keyword,
            int page,
            int size
    ) {
        validateBrandExists(brandId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<Menu> result = findMenusByFilters(brandId, category, keyword, pageable);

        List<MenuListItemResponse> content = result.getContent().stream()
                .map(this::toMenuListItem)
                .collect(Collectors.toList());

        return new PageResponse<>(content, result.getNumber(), result.hasNext());
    }

    public PageResponse<MenuSearchResponse> searchMenus(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<Menu> result = menuRepository
                .findByNameContainingIgnoreCaseAndIsActiveTrue(keyword, pageable);

        List<MenuSearchResponse> content = result.getContent().stream()
                .map(this::toMenuSearch)
                .collect(Collectors.toList());

        return new PageResponse<>(content, result.getNumber(), result.hasNext());
    }

    public MenuDetailResponse findMenuDetail(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found: " + menuId));

        List<TemperatureType> temperatures = menuTemperatureRepository
                .findByMenuIdAndIsActiveTrue(menuId)
                .stream()
                .map(MenuTemperature::getTemperature)
                .distinct()
                .toList();

        return new MenuDetailResponse(
                menu.getId(),
                menu.getBrand().getId(),
                menu.getBrand().getName(),
                menu.getName(),
                menu.getCategory(),
                menu.getDescription(),
                menu.getImageUrl(),
                temperatures
        );
    }

    public List<MenuSizeResponse> findMenuSizes(Long menuId, TemperatureType temperature) {
        MenuTemperature menuTemperature = menuTemperatureRepository
                .findByMenuIdAndTemperatureAndIsActiveTrue(menuId, temperature)
                .orElseThrow(() -> new IllegalArgumentException("MenuTemperature not found"));

        return menuSizeRepository.findByMenuTemperatureIdOrderByIdAsc(menuTemperature.getId())
                .stream()
                .map(this::toMenuSize)
                .toList();
    }

    public MenuSizeDetailResponse findMenuSizeDetail(Long menuSizeId) {
        MenuSize menuSize = menuSizeRepository.findDetailById(menuSizeId)
                .orElseThrow(() -> new IllegalArgumentException("MenuSize not found: " + menuSizeId));

        Menu menu = menuSize.getMenuTemperature().getMenu();

        return new MenuSizeDetailResponse(
                menuSize.getId(),
                menu.getId(),
                menu.getName(),
                menu.getBrand().getName(),
                menuSize.getMenuTemperature().getTemperature(),
                menuSize.getSizeName(),
                menuSize.getVolumeMl(),
                toNutrition(menuSize)
        );
    }

    private void validateBrandExists(Long brandId) {
        if (brandRepository.existsById(brandId)) {
            return;
        }
        throw new IllegalArgumentException("Brand not found: " + brandId);
    }

    private Page<Menu> findMenusByFilters(
            Long brandId,
            MenuCategory category,
            String keyword,
            Pageable pageable
    ) {
        if (isBlank(keyword) && category == null) {
            return menuRepository.findByBrandIdAndIsActiveTrue(brandId, pageable);
        }

        if (isBlank(keyword)) {
            return menuRepository.findByBrandIdAndCategoryAndIsActiveTrue(brandId, category, pageable);
        }

        if (category == null) {
            return menuRepository.findByBrandIdAndNameContainingIgnoreCaseAndIsActiveTrue(brandId, keyword, pageable);
        }

        return menuRepository
                .findByBrandIdAndCategoryAndNameContainingIgnoreCaseAndIsActiveTrue(brandId, category, keyword, pageable);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private MenuListItemResponse toMenuListItem(Menu menu) {
        return new MenuListItemResponse(menu.getId(), menu.getName(), menu.getCategory(), menu.getImageUrl());
    }

    private MenuSearchResponse toMenuSearch(Menu menu) {
        Brand brand = menu.getBrand();
        return new MenuSearchResponse(menu.getId(), brand.getName(), menu.getName(), menu.getImageUrl());
    }

    private MenuSizeResponse toMenuSize(MenuSize menuSize) {
        return new MenuSizeResponse(
                menuSize.getId(),
                menuSize.getSizeName(),
                menuSize.getVolumeMl(),
                toNutrition(menuSize)
        );
    }

    private NutritionResponse toNutrition(MenuSize menuSize) {
        return NutritionResponse.from(menuSize.getNutrition());
    }
}
