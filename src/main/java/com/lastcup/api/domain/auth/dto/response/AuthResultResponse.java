package com.lastcup.api.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResultResponse(
        @Schema(description = "유저 요약")
        UserSummaryResponse user,

        @Schema(description = "인증 토큰")
        AuthTokensResponse tokens
) {
}
