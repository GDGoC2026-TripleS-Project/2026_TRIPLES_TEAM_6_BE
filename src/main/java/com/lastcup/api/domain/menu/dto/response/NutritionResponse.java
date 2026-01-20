package com.lastcup.api.domain.menu.dto.response;

import com.lastcup.api.domain.menu.domain.Nutrition;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "영양 성분")
public record NutritionResponse(
        @Schema(description = "카페인(mg)", example = "85")
        Integer caffeineMg,

        @Schema(description = "당류(g)", example = "27")
        Integer sugarG,

        @Schema(description = "칼로리", example = "215")
        Integer calories,

        @Schema(description = "나트륨(mg)", example = "140")
        Integer sodiumMg,

        @Schema(description = "단백질(g)", example = "0")
        Integer proteinG,

        @Schema(description = "지방(g)", example = "0")
        Integer fatG
) {

    public static NutritionResponse from(Nutrition nutrition) {
        return new NutritionResponse(
                nutrition.getCaffeineMg(),
                nutrition.getSugarG(),
                nutrition.getCalories(),
                nutrition.getSodiumMg(),
                nutrition.getProteinG(),
                nutrition.getFatG()
        );
    }
}
