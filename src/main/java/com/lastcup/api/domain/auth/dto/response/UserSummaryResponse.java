package com.lastcup.api.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserSummaryResponse(
        @Schema(description = "유저 ID", example = "1")
        Long id,

        @Schema(description = "닉네임", example = "똑똑한 기린")
        String nickname
) {
}
