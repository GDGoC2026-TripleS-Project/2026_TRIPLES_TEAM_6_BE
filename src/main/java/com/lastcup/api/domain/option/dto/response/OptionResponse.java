package com.lastcup.api.domain.option.dto.response;

import com.lastcup.api.domain.option.domain.Option;
import com.lastcup.api.domain.option.domain.OptionCategory;
import com.lastcup.api.domain.option.domain.OptionSelectionType;
import io.swagger.v3.oas.annotations.media.Schema;

public record OptionResponse(
        @Schema(description = "옵션 ID", example = "1")
        Long id,

        @Schema(description = "옵션명", example = "바닐라 시럽")
        String name,

        @Schema(description = "옵션 카테고리", example = "SYRUP")
        OptionCategory category,

        @Schema(description = "옵션 선택 방식", example = "COUNT")
        OptionSelectionType selectionType,

        @Schema(description = "옵션 1개당 카페인(mg). 확인 페이지 미리보기 계산용. 영양 정보가 없는 옵션은 null", example = "75")
        Integer caffeineMg
) {
    public static OptionResponse from(Option option) {
        Integer caffeineMg = option.getNutrition() != null
                ? option.getNutrition().getCaffeineMg()
                : null;

        return new OptionResponse(
                option.getId(),
                option.getName(),
                option.getCategory(),
                option.getSelectionType(),
                caffeineMg
        );
    }
}
