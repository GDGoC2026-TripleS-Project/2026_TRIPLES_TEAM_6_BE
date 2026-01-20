package com.lastcup.api.domain.brand.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "브랜드 목록 아이템")
public record BrandResponse(
        @Schema(description = "브랜드 ID", example = "1")
        Long id,

        @Schema(description = "브랜드명", example = "스타벅스")
        String name,

        @Schema(description = "브랜드 로고 URL", example = "https://...")
        String logoUrl,

        @Schema(description = "즐겨찾기 여부(인증 시 유동)", example = "true")
        boolean isFavorite
) {
}
