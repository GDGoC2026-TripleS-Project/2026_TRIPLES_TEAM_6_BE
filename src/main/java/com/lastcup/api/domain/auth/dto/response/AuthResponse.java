package com.lastcup.api.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(description = "유저 요약")
        UserSummaryResponse user,

        @Schema(description = "인증 토큰")
        AuthTokensResponse tokens,

        @Schema(description = "신규 유저 여부", example = "true")
        boolean isNewUser
) {
}
