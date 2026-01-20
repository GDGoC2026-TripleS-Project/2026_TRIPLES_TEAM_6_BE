package com.lastcup.api.domain.menu.dto.response;

import com.lastcup.api.domain.menu.domain.MenuCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "브랜드 내 메뉴 목록 아이템")
public record MenuListItemResponse(
        @Schema(description = "메뉴 ID", example = "10")
        Long id,

        @Schema(description = "메뉴명", example = "카페 아메리카노")
        String name,

        @Schema(description = "카테고리", example = "COFFEE")
        MenuCategory category,

        @Schema(description = "이미지 URL", example = "https://...")
        String imageUrl
) {
}
