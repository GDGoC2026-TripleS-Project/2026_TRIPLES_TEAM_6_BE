package com.lastcup.api.domain.intake.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "음료 종류별 통계 항목")
public record DrinkGroupResponse(

        @Schema(description = "그룹에 포함된 섭취 기록 ID 목록", example = "[101, 102, 103]")
        List<Long> intakeIds,

        @Schema(description = "브랜드명", example = "스타벅스")
        String brandName,

        @Schema(description = "메뉴명", example = "블랙 글레이즈드 라떼")
        String menuName,

        @Schema(description = "온도", example = "ICED")
        String temperature,

        @Schema(description = "사이즈명", example = "Tall")
        String sizeName,

        @Schema(description = "카페인(mg) — 그룹 총합", example = "1600")
        int caffeineSnapshot,

        @Schema(description = "당류(g) — 그룹 총합", example = "180")
        int sugarSnapshot,

        @Schema(description = "에스프레소 환산 잔 수 (75mg = 1잔)", example = "21")
        int espressoShotCount,

        @Schema(description = "각설탕 환산 개수 (3g = 1개)", example = "60")
        int sugarCubeCount,

        @Schema(description = "잔 수", example = "20")
        int quantity,

        @Schema(description = "추가 옵션 목록")
        List<IntakeOptionDetailResponse> options,

        @Schema(description = "1잔당 카페인(mg)", example = "80")
        int caffeinePerUnit,

        @Schema(description = "1잔당 당류(g)", example = "9")
        int sugarPerUnit
) {
}
