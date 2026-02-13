package com.lastcup.api.global.response;

import static com.lastcup.api.global.config.AppTimeZone.KST;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "API 표준 응답 Envelope")
public record ApiResponse<T>(
        @Schema(description = "성공 여부", example = "true")
        boolean success,
        @Schema(description = "응답 데이터")
        T data,
        @Schema(description = "에러 정보")
        ApiError error,
        @Schema(description = "응답 생성 시각(ISO-8601)", example = "2026-01-17T21:30:00")
        String timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, LocalDateTime.now(KST).toString());
    }

    public static ApiResponse<Void> failure(ApiError error) {
        return new ApiResponse<>(false, null, error, LocalDateTime.now(KST).toString());
    }
}
