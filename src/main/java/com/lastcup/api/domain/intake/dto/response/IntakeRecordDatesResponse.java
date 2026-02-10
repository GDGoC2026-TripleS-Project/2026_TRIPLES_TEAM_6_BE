package com.lastcup.api.domain.intake.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "기간 내 섭취 기록 존재 날짜 응답")
public record IntakeRecordDatesResponse(

        @Schema(description = "시작 날짜", example = "2026-01-01")
        LocalDate startDate,

        @Schema(description = "종료 날짜", example = "2026-01-31")
        LocalDate endDate,

        @Schema(description = "섭취 기록이 존재하는 날짜 목록")
        List<LocalDate> dates
) {
}
