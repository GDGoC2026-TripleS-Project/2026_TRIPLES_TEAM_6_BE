package com.lastcup.api.domain.menu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메뉴 사이즈 목록 아이템")
public record MenuSizeResponse(
        @Schema(description = "메뉴 사이즈 ID", example = "100")
        Long menuSizeId,

        @Schema(description = "사이즈명", example = "Tall")
        String sizeName,

        @Schema(description = "용량(ml)", example = "355")
        Integer volumeMl,

        @Schema(description = "영양성분")
        NutritionResponse nutrition
) {
}
