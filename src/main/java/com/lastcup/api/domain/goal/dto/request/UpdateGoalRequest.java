package com.lastcup.api.domain.goal.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record UpdateGoalRequest(
        @Schema(description = "일일 카페인 목표(mg)", example = "400")
        @NotNull
        @Min(0)
        Integer dailyCaffeineTarget,

        @Schema(description = "일일 당류 목표(g)", example = "25")
        @NotNull
        @Min(0)
        Integer dailySugarTarget,

        @Schema(description = "적용 시작일(YYYY-MM-DD). 없으면 오늘", example = "2026-02-05")
        LocalDate startDate
) {
}
