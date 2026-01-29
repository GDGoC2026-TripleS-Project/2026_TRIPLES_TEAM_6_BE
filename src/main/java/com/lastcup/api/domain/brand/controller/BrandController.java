package com.lastcup.api.domain.brand.controller;

import com.lastcup.api.global.response.ApiResponse;
import com.lastcup.api.domain.brand.dto.response.BrandResponse;
import com.lastcup.api.domain.brand.service.BrandService;
import com.lastcup.api.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Brand", description = "브랜드 조회 API")
@RestController
@RequestMapping("/api/v1/brands")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @Operation(
            summary = "브랜드 목록 조회",

            description = "인증 Optional. 토큰이 있으면 즐겨찾기 우선 정렬 + isFavorite 반영, 없으면 이름순 정렬 + isFavorite=false"
    )
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping
    public ApiResponse<List<BrandResponse>> findBrands(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String keyword
    ) {
        Long userId = getUserIdOrNull(authUser);
        return ApiResponse.success(brandService.findBrands(keyword, userId));
    }

    private Long getUserIdOrNull(AuthUser authUser) {
        if (authUser == null) {
            return null;
        }
        return authUser.userId();
    }
}
