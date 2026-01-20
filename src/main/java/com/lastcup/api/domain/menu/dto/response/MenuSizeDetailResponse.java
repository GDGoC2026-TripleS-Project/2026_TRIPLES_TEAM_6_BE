package com.lastcup.api.domain.menu.dto.response;

import com.lastcup.api.domain.menu.domain.TemperatureType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메뉴 사이즈 상세")
public record MenuSizeDetailResponse(
        @Schema(description = "메뉴 사이즈 ID", example = "100")
        Long menuSizeId,

        @Schema(description = "메뉴 ID", example = "10")
        Long menuId,

        @Schema(description = "메뉴명", example = "카페 아메리카노")
        String menuName,

        @Schema(description = "브랜드명", example = "스타벅스")
        String brandName,

        @Schema(description = "온도", example = "HOT")
        TemperatureType temperature,

        @Schema(description = "사이즈명", example = "Tall")
        String sizeName,

        @Schema(description = "용량(ml)", example = "355")
        Integer volumeMl,

        @Schema(description = "영양성분")
        NutritionResponse nutrition
) {
}
