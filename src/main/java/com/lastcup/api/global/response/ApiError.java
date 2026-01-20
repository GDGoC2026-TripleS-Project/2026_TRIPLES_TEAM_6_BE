package com.lastcup.api.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "API 표준 에러 정보")
public record ApiError(
        @Schema(description = "에러 코드", example = "COMMON_VALIDATION_FAILED")
        String code,
        @Schema(description = "에러 메시지", example = "요청 값이 유효하지 않습니다.")
        String message,
        @Schema(description = "필드 검증 오류 목록")
        List<FieldError> fieldErrors
) {
    @Schema(description = "필드 오류 상세")
    public record FieldError(
            @Schema(description = "필드 이름", example = "loginId")
            String field,
            @Schema(description = "오류 사유", example = "필수 입력값입니다.")
            String reason,
            @Schema(description = "거부된 값", example = " ")
            Object rejectedValue
    ) {
    }
}
