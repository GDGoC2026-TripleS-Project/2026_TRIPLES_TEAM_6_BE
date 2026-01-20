package com.lastcup.api.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record BrandFavoriteResponse(
        @Schema(description = "즐겨찾기 등록 성공 여부", example = "true")
        boolean favorited
) {
    public static BrandFavoriteResponse success() {
        return new BrandFavoriteResponse(true);
    }
}
