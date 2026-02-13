package com.lastcup.api.global.response;

import static com.lastcup.api.global.config.AppTimeZone.KST;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(name = "ApiResponseError", description = "API 표준 에러 응답 Envelope")
public record ApiResponseError(
        @Schema(description = "성공 여부", example = "false")
        boolean success,
        @Schema(description = "응답 데이터", nullable = true)
        Void data,
        @Schema(description = "에러 정보")
        ApiError error,
        @Schema(description = "응답 생성 시각(ISO-8601)", example = "2026-01-17T21:30:00")
        String timestamp
) {
    public static ApiResponseError from(ApiError error) {
        return new ApiResponseError(false, null, error, LocalDateTime.now(KST).toString());
    }
}
