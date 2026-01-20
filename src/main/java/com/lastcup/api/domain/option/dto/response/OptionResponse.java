package com.lastcup.api.domain.option.dto.response;

import com.lastcup.api.domain.option.domain.Option;
import com.lastcup.api.domain.option.domain.OptionCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record OptionResponse(
        @Schema(description = "옵션 ID", example = "1")
        Long id,

        @Schema(description = "옵션명", example = "바닐라 시럽")
        String name,

        @Schema(description = "옵션 카테고리", example = "SYRUP")
        OptionCategory category,

        @Schema(description = "1회 추가당 카페인(mg)", example = "0")
        int caffeineMg,

        @Schema(description = "1회 추가당 당류(g)", example = "5")
        int sugarG,

        @Schema(description = "칼로리", example = "20", nullable = true)
        Integer calories,

        @Schema(description = "나트륨(mg)", example = "0", nullable = true)
        Integer sodiumMg,

        @Schema(description = "단백질(g)", example = "0", nullable = true)
        Integer proteinG,

        @Schema(description = "지방(g)", example = "0", nullable = true)
        Integer fatG,

        @Schema(description = "표시 단위(펌프/샷/스쿱 등)", example = "펌프", nullable = true)
        String displayUnitName,

        @Schema(description = "각설탕 환산값", example = "1.50", nullable = true)
        BigDecimal sugarCubeEquivalent
) {
    public static OptionResponse from(Option option) {
        return new OptionResponse(
                option.getId(),
                option.getName(),
                option.getCategory(),
                option.getCaffeineMg(),
                option.getSugarG(),
                option.getCalories(),
                option.getSodiumMg(),
                option.getProteinG(),
                option.getFatG(),
                option.getDisplayUnitName(),
                option.getSugarCubeEquivalent()
        );
    }
}
