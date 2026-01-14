package com.lastcup.api.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(
        @Schema(description = "소셜 제공자 토큰(권장: Google ID Token)", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6...")
        @NotBlank
        String providerAccessToken
) {
}
