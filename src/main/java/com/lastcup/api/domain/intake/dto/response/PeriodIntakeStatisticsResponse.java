package com.lastcup.api.domain.intake.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "기간별 음료 통계 응답")
public record PeriodIntakeStatisticsResponse(

        @Schema(description = "시작 날짜", example = "2026-01-08")
        LocalDate startDate,

        @Schema(description = "종료 날짜", example = "2026-01-13")
        LocalDate endDate,

        @Schema(description = "총 카페인 섭취량(mg)", example = "1268")
        int totalCaffeine,

        @Schema(description = "총 당류 섭취량(g)", example = "932")
        int totalSugar,

        @Schema(description = "총 에스프레소 환산 잔 수 (75mg = 1잔)", example = "17")
        int totalEspressoShotCount,

        @Schema(description = "총 각설탕 환산 개수 (3g = 1개)", example = "311")
        int totalSugarCubeCount,

        @Schema(description = "총 잔 수", example = "34")
        int intakeCount,

        @Schema(description = "음료 종류별 섭취 목록 (잔 수 내림차순)")
        List<DrinkGroupResponse> intakes
) {
}
