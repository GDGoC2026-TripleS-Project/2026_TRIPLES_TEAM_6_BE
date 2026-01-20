package com.lastcup.api.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record BrandUnfavoriteResponse(
        @Schema(description = "즐겨찾기 삭제 성공 여부", example = "true")
        boolean deleted
) {
    public static BrandUnfavoriteResponse success() {
        return new BrandUnfavoriteResponse(true);
    }
}
