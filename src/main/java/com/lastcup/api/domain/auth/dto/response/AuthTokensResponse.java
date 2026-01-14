package com.lastcup.api.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthTokensResponse(
        @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken
) {
}
