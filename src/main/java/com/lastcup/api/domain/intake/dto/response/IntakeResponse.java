package com.lastcup.api.domain.intake.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "섭취 기록 응답")
public record IntakeResponse(

        @Schema(description = "섭취 기록 ID", example = "1")
        Long id,

        @Schema(description = "유저 ID", example = "10")
        Long userId,

        @Schema(description = "섭취 날짜", example = "2026-01-24")
        LocalDate intakeDate,

        @Schema(description = "메뉴 사이즈 ID", example = "42")
        Long menuSizeId,

        @Schema(description = "수량", example = "1")
        int quantity,

        @Schema(description = "카페인 스냅샷(mg)", example = "150")
        int caffeineSnapshot,

        @Schema(description = "당류 스냅샷(g)", example = "3")
        int sugarSnapshot,

        @Schema(description = "에스프레소 환산 잔 수 (75mg = 1잔)", example = "2")
        int espressoShotCount,

        @Schema(description = "각설탕 환산 개수 (3g = 1개)", example = "1")
        int sugarCubeCount,

        @Schema(description = "칼로리 스냅샷(kcal)")
        Integer caloriesSnapshot,

        @Schema(description = "나트륨 스냅샷(mg)")
        Integer sodiumSnapshot,

        @Schema(description = "단백질 스냅샷(g)")
        Integer proteinSnapshot,

        @Schema(description = "지방 스냅샷(g)")
        Integer fatSnapshot,

        @Schema(description = "목표 카페인(mg)", example = "400")
        int goalCaffeineTargetSnapshot,

        @Schema(description = "목표 당류(g)", example = "25")
        int goalSugarTargetSnapshot,

        @Schema(description = "추가 옵션 목록")
        List<IntakeOptionResponse> options,

        @Schema(description = "생성 시각")
        LocalDateTime createdAt
) {
}
