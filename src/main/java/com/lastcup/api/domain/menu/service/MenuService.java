package com.lastcup.api.domain.menu.service;

import com.lastcup.api.domain.brand.repository.BrandRepository;
import com.lastcup.api.domain.menu.domain.Menu;
import com.lastcup.api.domain.menu.domain.MenuCategory;
import com.lastcup.api.domain.menu.domain.MenuSize;
import com.lastcup.api.domain.menu.domain.MenuTemperature;
import com.lastcup.api.domain.menu.domain.TemperatureType;
import com.lastcup.api.domain.menu.dto.response.MenuDetailResponse;
import com.lastcup.api.domain.menu.dto.response.MenuFavoriteItemResponse;
import com.lastcup.api.domain.menu.dto.response.MenuListItemResponse;
import com.lastcup.api.domain.menu.dto.response.MenuSearchResponse;
import com.lastcup.api.domain.menu.dto.response.MenuSizeDetailResponse;
import com.lastcup.api.domain.menu.dto.response.MenuSizeResponse;
import com.lastcup.api.domain.menu.mapper.MenuMapper;
import com.lastcup.api.domain.menu.repository.MenuRepository;
import com.lastcup.api.domain.menu.repository.MenuSizeRepository;
import com.lastcup.api.domain.menu.repository.MenuTemperatureRepository;
import com.lastcup.api.domain.user.repository.MenuFavoriteRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final MenuMapper menuMapper;
    private final MenuFavoriteRepository menuFavoriteRepository;

    public MenuService(
            BrandRepository brandRepository,
            MenuRepository menuRepository,
            MenuTemperatureRepository menuTemperatureRepository,
            MenuSizeRepository menuSizeRepository,
            MenuMapper menuMapper,
            MenuFavoriteRepository menuFavoriteRepository
    ) {
        this.brandRepository = brandRepository;
        this.menuRepository = menuRepository;
        this.menuTemperatureRepository = menuTemperatureRepository;
        this.menuSizeRepository = menuSizeRepository;
        this.menuMapper = menuMapper;
        this.menuFavoriteRepository = menuFavoriteRepository;
    }

    public List<MenuListItemResponse> findBrandMenus(
            Long brandId,
            MenuCategory category,
            String keyword,
            Long userId
    ) {
        validateBrandExists(brandId);

        Sort sort = Sort.by(Sort.Direction.ASC, "name");
        List<Menu> menus = findMenusByFilters(brandId, category, keyword, sort);
        Set<Long> favoriteMenuIds = findFavoriteMenuIds(userId);

        List<MenuListItemResponse> result = menus.stream()
                .map(menu ->
                        menuMapper.toMenuListItem(
                                menu,
                                favoriteMenuIds.contains(menu.getId())
                        )
                )
                .collect(Collectors.toList());

        sortByFavoriteThenName(
                userId,
                result,
                MenuListItemResponse::isFavorite,
                MenuListItemResponse::name
        );

        return result;
    }

    public List<MenuSearchResponse> searchMenus(
            String keyword,
            Long userId
    ) {
        Sort sort = Sort.by(Sort.Direction.ASC, "name");
        List<Menu> menus =
                menuRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(keyword, sort);

        Set<Long> favoriteMenuIds = findFavoriteMenuIds(userId);

        List<MenuSearchResponse> result = menus.stream()
                .map(menu ->
                        menuMapper.toMenuSearch(
                                menu,
                                favoriteMenuIds.contains(menu.getId())
                        )
                )
                .collect(Collectors.toList());

        if (userId != null) {
            result = result.stream()
                    .sorted(
                            Comparator.comparing(MenuSearchResponse::isFavorite).reversed()
                                    .thenComparing(MenuSearchResponse::brandName)
                                    .thenComparing(MenuSearchResponse::name)
                    )
                    .toList();
        }

        return result;
    }

    public MenuDetailResponse findMenuDetail(Long menuId) {
        Menu menu = menuRepository.findWithBrandById(menuId)
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

        return menuSizeRepository
                .findByMenuTemperatureIdOrderByIdAsc(menuTemperature.getId())
                .stream()
                .map(menuMapper::toMenuSize)
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
                menuMapper.toMenuSize(menuSize).nutrition()
        );
    }

    public List<MenuFavoriteItemResponse> findFavoriteMenus(Long userId) {
        validateUserId(userId);

        List<Long> menuIds = menuFavoriteRepository.findMenuIdsByUserId(userId);
        if (menuIds.isEmpty()) {
            return List.of();
        }

        return menuRepository.findByIdInWithBrandAndIsActiveTrue(menuIds).stream()
                .map(menuMapper::toMenuFavoriteItem)
                .sorted(
                        Comparator.comparing(MenuFavoriteItemResponse::brandName)
                                .thenComparing(MenuFavoriteItemResponse::name)
                )
                .toList();
    }

    private void validateBrandExists(Long brandId) {
        if (!brandRepository.existsById(brandId)) {
            throw new IllegalArgumentException("Brand not found: " + brandId);
        }
    }

    private List<Menu> findMenusByFilters(
            Long brandId,
            MenuCategory category,
            String keyword,
            Sort sort
    ) {
        if (isBlank(keyword) && category == null) {
            return menuRepository.findByBrandIdAndIsActiveTrue(brandId, sort);
        }
        if (isBlank(keyword)) {
            return menuRepository.findByBrandIdAndCategoryAndIsActiveTrue(brandId, category, sort);
        }
        if (category == null) {
            return menuRepository.findByBrandIdAndNameContainingIgnoreCaseAndIsActiveTrue(
                    brandId, keyword, sort
            );
        }
        return menuRepository.findByBrandIdAndCategoryAndNameContainingIgnoreCaseAndIsActiveTrue(
                brandId, category, keyword, sort
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Set<Long> findFavoriteMenuIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        return menuFavoriteRepository.findMenuIdsByUserId(userId)
                .stream()
                .collect(Collectors.toSet());
    }

    private <T> void sortByFavoriteThenName(
            Long userId,
            List<T> content,
            java.util.function.Function<T, Boolean> favoriteExtractor,
            java.util.function.Function<T, String> nameExtractor
    ) {
        if (userId == null) {
            return;
        }
        content.sort(
                Comparator.comparing(favoriteExtractor).reversed()
                        .thenComparing(nameExtractor)
        );
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
    }
}
