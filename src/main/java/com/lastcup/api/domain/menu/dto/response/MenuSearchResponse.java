package com.lastcup.api.domain.menu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메뉴 검색 결과 아이템")
public record MenuSearchResponse(
        @Schema(description = "메뉴 ID", example = "10")
        Long id,

        @Schema(description = "브랜드명", example = "스타벅스")
        String brandName,

        @Schema(description = "메뉴명", example = "카페 라떼")
        String name,

        @Schema(description = "이미지 URL", example = "https://...")
        String imageUrl
) {
}
