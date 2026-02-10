package com.lastcup.api.domain.intake.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "섭취 이력 항목")
public record IntakeHistoryItemResponse(

        @Schema(description = "섭취 기록 ID", example = "1")
        Long id,

        @Schema(description = "섭취 날짜", example = "2026-01-26")
        LocalDate intakeDate,

        @Schema(description = "브랜드명", example = "이디야")
        String brandName,

        @Schema(description = "메뉴명", example = "에스프레소 쉐이크")
        String menuName,

        @Schema(description = "온도", example = "ICED")
        String temperature,

        @Schema(description = "사이즈명", example = "Large")
        String sizeName,

        @Schema(description = "카페인(mg)", example = "158")
        int caffeineSnapshot,

        @Schema(description = "당류(g)", example = "19")
        int sugarSnapshot,

        @Schema(description = "에스프레소 환산 잔 수 (75mg = 1잔)", example = "2")
        int espressoShotCount,

        @Schema(description = "각설탕 환산 개수 (3g = 1개)", example = "6")
        int sugarCubeCount,

        @Schema(description = "수량", example = "1")
        int quantity,

        @Schema(description = "추가 옵션 목록")
        List<IntakeOptionDetailResponse> options,

        @Schema(description = "기록 시각")
        LocalDateTime createdAt
) {
}
