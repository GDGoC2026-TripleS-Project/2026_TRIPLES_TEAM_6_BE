package com.lastcup.api.domain.intake.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "섭취 옵션 상세 응답")
public record IntakeOptionDetailResponse(

        @Schema(description = "옵션 ID", example = "5")
        Long optionId,

        @Schema(description = "옵션명", example = "에스프레소 샷 추가")
        String optionName,

        @Schema(description = "수량", example = "2")
        int quantity,

        @Schema(description = "옵션 1개당 카페인(mg). 영양 정보가 없는 옵션은 null", example = "75")
        Integer caffeineMg
) {
}
