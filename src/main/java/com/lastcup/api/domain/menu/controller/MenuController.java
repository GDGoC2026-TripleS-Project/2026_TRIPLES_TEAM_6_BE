package com.lastcup.api.domain.menu.controller;

import com.lastcup.api.global.response.ApiResponse;
import com.lastcup.api.domain.menu.domain.MenuCategory;
import com.lastcup.api.domain.menu.domain.TemperatureType;
import com.lastcup.api.domain.menu.dto.response.MenuDetailResponse;
import com.lastcup.api.domain.menu.dto.response.MenuFavoriteItemResponse;
import com.lastcup.api.domain.menu.dto.response.MenuListItemResponse;
import com.lastcup.api.domain.menu.dto.response.MenuSearchResponse;
import com.lastcup.api.domain.menu.dto.response.MenuSizeDetailResponse;
import com.lastcup.api.domain.menu.dto.response.MenuSizeResponse;
import com.lastcup.api.domain.menu.service.MenuService;
import com.lastcup.api.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Menu", description = "메뉴/사이즈 조회 API")
@RestController
@RequestMapping("/api/v1")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @Operation(
            summary = "특정 브랜드의 메뉴 목록 조회",
            description = "brandId 범위 내에서 category 1차 필터, keyword 2차 필터."
    )
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/brands/{brandId}/menus")
    public ApiResponse<List<MenuListItemResponse>> findBrandMenus(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long brandId,
            @RequestParam(required = false) MenuCategory category,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(menuService.findBrandMenus(brandId, category, keyword, getUserIdOrNull(authUser)));
    }

    @Operation(
            summary = "메뉴 전체 검색(브랜드 무관)",
            description = "keyword로 전체 메뉴 검색."
    )
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/menus/search")
    public ApiResponse<List<MenuSearchResponse>> searchMenus(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam String keyword

    ) {
        return ApiResponse.success(menuService.searchMenus(keyword, getUserIdOrNull(authUser)));
    }

    @Operation(
            summary = "메뉴 상세 조회",
            description = "availableTemperatures(HOT/ICED) 포함"
    )
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/menus/{menuId}")
    public ApiResponse<MenuDetailResponse> findMenuDetail(@PathVariable Long menuId) {
        return ApiResponse.success(menuService.findMenuDetail(menuId));
    }

    @Operation(
            summary = "메뉴+온도별 사이즈 목록 조회",
            description = "temperature 필수. 각 사이즈에 nutrition 포함"
    )
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/menus/{menuId}/sizes")
    public ApiResponse<List<MenuSizeResponse>> findMenuSizes(
            @PathVariable Long menuId,
            @Parameter(description = "온도", example = "HOT")
            @RequestParam TemperatureType temperature
    ) {
        return ApiResponse.success(menuService.findMenuSizes(menuId, temperature));
    }

    @Operation(
            summary = "특정 사이즈 상세 조회",
            description = "menu/brand/temperature/size + nutrition 포함"
    )
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/menus/sizes/{menuSizeId}")
    public ApiResponse<MenuSizeDetailResponse> findMenuSizeDetail(@PathVariable Long menuSizeId) {
        return ApiResponse.success(menuService.findMenuSizeDetail(menuSizeId));
    }

    @Operation(summary = "메뉴 즐겨찾기 목록 조회", description = "사용자의 즐겨찾기 메뉴 목록을 반환합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/menus/favorites")
    public ApiResponse<List<MenuFavoriteItemResponse>> findFavoriteMenus(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return ApiResponse.success(menuService.findFavoriteMenus(authUser.userId()));
    }

    private Long getUserIdOrNull(AuthUser authUser) {
        if (authUser == null) {
            return null;
        }
        return authUser.userId();
    }
}
