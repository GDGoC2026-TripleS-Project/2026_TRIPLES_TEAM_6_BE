package com.lastcup.api.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record SocialLoginRequest(
        @Schema(description = "소셜 제공자 토큰 (GOOGLE은 ID Token 사용)", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6...")
        String providerAccessToken,
        @Schema(description = "소셜 인가 코드 (KAKAO/APPLE 로그인 시 사용)", example = "ABCD1234")
        String authorizationCode,
        @Schema(description = "애플 ID 토큰 (APPLE 로그인 시 선택 전달)", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6...")
        String identityToken,
        @Schema(description = "애플 사용자 이메일 (APPLE 최초 1회 제공, 선택 전달)", example = "user@example.com")
        String email,
        @Schema(description = "애플 사용자 이름 (APPLE 최초 1회 제공, 선택 전달)", example = "홍길동")
        String name
) {
}
