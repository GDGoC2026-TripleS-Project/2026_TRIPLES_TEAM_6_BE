package com.lastcup.api.domain.menu.dto.response;

import com.lastcup.api.domain.menu.domain.MenuCategory;
import com.lastcup.api.domain.menu.domain.TemperatureType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "메뉴 상세")
public record MenuDetailResponse(
        @Schema(description = "메뉴 ID", example = "10")
        Long id,

        @Schema(description = "브랜드 ID", example = "1")
        Long brandId,

        @Schema(description = "브랜드명", example = "스타벅스")
        String brandName,

        @Schema(description = "메뉴명", example = "카페 아메리카노")
        String name,

        @Schema(description = "카테고리", example = "COFFEE")
        MenuCategory category,

        @Schema(description = "설명", example = "진한 원두의 풍미")
        String description,

        @Schema(description = "이미지 URL", example = "https://...")
        String imageUrl,

        @Schema(description = "제공 온도 목록", example = "[\"HOT\",\"ICED\"]")
        List<TemperatureType> availableTemperatures
) {
}
