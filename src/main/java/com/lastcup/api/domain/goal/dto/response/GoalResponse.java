package com.lastcup.api.domain.goal.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "목표 섭취량 응답")
public record GoalResponse(
        @Schema(description = "목표 ID", example = "1")
        Long id,

        @Schema(description = "유저 ID", example = "10")
        Long userId,

        @Schema(description = "일일 카페인 목표(mg)", example = "400")
        int dailyCaffeineTarget,

        @Schema(description = "카페인 목표의 에스프레소 환산 잔 수 (75mg = 1잔)", example = "5")
        int dailyCaffeineEspressoShotCount,

        @Schema(description = "일일 당류 목표(g)", example = "25")
        int dailySugarTarget,

        @Schema(description = "당류 목표의 각설탕 환산 개수 (3g = 1개)", example = "8")
        int dailySugarCubeCount,

        @Schema(description = "적용 시작일", example = "2026-02-05")
        LocalDate startDate,

        @Schema(description = "적용 종료일(null이면 현재 적용)")
        LocalDate endDate,

        @Schema(description = "생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "수정 시각")
        LocalDateTime updatedAt
) {
}
