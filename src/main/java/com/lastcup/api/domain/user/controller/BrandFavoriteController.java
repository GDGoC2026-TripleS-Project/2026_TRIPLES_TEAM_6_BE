package com.lastcup.api.domain.user.controller;

import com.lastcup.api.global.response.ApiResponse;
import com.lastcup.api.domain.user.dto.response.BrandFavoriteResponse;
import com.lastcup.api.domain.user.dto.response.BrandUnfavoriteResponse;
import com.lastcup.api.domain.user.service.BrandFavoriteService;
import com.lastcup.api.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "BrandFavorite", description = "브랜드 즐겨찾기 API")
@RestController
@RequestMapping("/api/v1/brands/{brandId}/favorites")
public class BrandFavoriteController {

    private final BrandFavoriteService brandFavoriteService;

    public BrandFavoriteController(BrandFavoriteService brandFavoriteService) {
        this.brandFavoriteService = brandFavoriteService;
    }

    @Operation(summary = "브랜드 즐겨찾기 등록", description = "해당 브랜드를 즐겨찾기에 등록합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping
    public ApiResponse<BrandFavoriteResponse> createBrandFavorite(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long brandId
    ) {
        brandFavoriteService.createBrandFavorite(authUser.userId(), brandId);
        return ApiResponse.success(BrandFavoriteResponse.success());
    }

    @Operation(summary = "브랜드 즐겨찾기 취소", description = "해당 브랜드를 즐겨찾기에서 삭제합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping
    public ApiResponse<BrandUnfavoriteResponse> deleteBrandFavorite(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long brandId
    ) {
        brandFavoriteService.deleteBrandFavorite(authUser.userId(), brandId);
        return ApiResponse.success(BrandUnfavoriteResponse.success());
    }
}
