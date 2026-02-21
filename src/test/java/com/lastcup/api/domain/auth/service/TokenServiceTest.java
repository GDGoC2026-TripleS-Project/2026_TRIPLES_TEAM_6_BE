package com.lastcup.api.domain.auth.service;

import com.lastcup.api.domain.auth.dto.response.AuthTokensResponse;
import com.lastcup.api.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TokenService 단위 테스트.
 * JwtProvider한테 토큰 생성을 넘기기만 하는 서비스라 테스트가 간단하다.
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock private JwtProvider jwtProvider;

    @InjectMocks
    private TokenService tokenService;

    // 1. createTokens — 토큰 쌍 생성

    @Nested
    @DisplayName("토큰 쌍 생성 (createTokens)")
    class CreateTokens {

        @Test
        @DisplayName("정상 생성 — accessToken + refreshToken 반환")
        void createTokensSuccess() {
            // given — JwtProvider가 각각의 토큰 문자열을 반환하도록 스텁
            when(jwtProvider.createAccessToken(1L)).thenReturn("access-token-value");
            when(jwtProvider.createRefreshToken(1L)).thenReturn("refresh-token-value");

            // when
            AuthTokensResponse response = tokenService.createTokens(1L);

            // then — 두 토큰이 올바르게 응답에 담기는지 검증
            assertEquals("access-token-value", response.accessToken());
            assertEquals("refresh-token-value", response.refreshToken());

            // 행위 검증: JwtProvider의 두 메서드가 정확히 1번씩 호출
            verify(jwtProvider).createAccessToken(1L);
            verify(jwtProvider).createRefreshToken(1L);
        }
    }
}
