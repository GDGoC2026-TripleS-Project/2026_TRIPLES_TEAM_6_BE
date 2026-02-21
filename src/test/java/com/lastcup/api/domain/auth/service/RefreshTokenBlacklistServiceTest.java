package com.lastcup.api.domain.auth.service;

import com.lastcup.api.domain.auth.domain.RefreshTokenBlacklist;
import com.lastcup.api.domain.auth.repository.RefreshTokenBlacklistRepository;
import com.lastcup.api.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RefreshTokenBlacklistService 단위 테스트.
 * refresh token 블랙리스트 등록/조회 로직을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenBlacklistServiceTest {

    @Mock private RefreshTokenBlacklistRepository refreshTokenBlacklistRepository;
    @Mock private JwtProvider jwtProvider;

    @InjectMocks
    private RefreshTokenBlacklistService refreshTokenBlacklistService;

    private static final String TOKEN = "test-refresh-token";
    private static final LocalDateTime FUTURE_EXPIRY = LocalDateTime.of(2099, 12, 31, 23, 59);

    // 1. blacklist — 리프레시 토큰 블랙리스트 등록

    @Nested
    @DisplayName("리프레시 토큰 블랙리스트 등록 (blacklist)")
    class Blacklist {

        @Test
        @DisplayName("정상 등록 — 새 토큰 블랙리스트에 저장")
        void blacklistNewToken() {
            // given
            when(jwtProvider.getRefreshTokenExpiresAt(TOKEN)).thenReturn(FUTURE_EXPIRY);
            when(refreshTokenBlacklistRepository.existsByTokenAndExpiresAtAfter(eq(TOKEN), any(LocalDateTime.class)))
                    .thenReturn(false);

            // when
            refreshTokenBlacklistService.blacklist(TOKEN);

            // then
            verify(refreshTokenBlacklistRepository).save(any(RefreshTokenBlacklist.class));
        }

        @Test
        @DisplayName("이미 블랙리스트에 존재 — 저장 스킵 (멱등성)")
        void blacklistAlreadyExists() {
            // given
            when(jwtProvider.getRefreshTokenExpiresAt(TOKEN)).thenReturn(FUTURE_EXPIRY);
            when(refreshTokenBlacklistRepository.existsByTokenAndExpiresAtAfter(eq(TOKEN), any(LocalDateTime.class)))
                    .thenReturn(true);

            // when
            refreshTokenBlacklistService.blacklist(TOKEN);

            // then
            verify(refreshTokenBlacklistRepository, never()).save(any());
        }
    }

    // 2. isBlacklisted — 블랙리스트 여부 확인

    @Nested
    @DisplayName("블랙리스트 여부 확인 (isBlacklisted)")
    class IsBlacklisted {

        @Test
        @DisplayName("블랙리스트에 존재 — true 반환")
        void isBlacklistedTrue() {
            // given
            when(refreshTokenBlacklistRepository.existsByTokenAndExpiresAtAfter(eq(TOKEN), any(LocalDateTime.class)))
                    .thenReturn(true);

            // when
            boolean result = refreshTokenBlacklistService.isBlacklisted(TOKEN);

            // then
            assertTrue(result);
        }

        @Test
        @DisplayName("블랙리스트에 없음 — false 반환")
        void isBlacklistedFalse() {
            // given
            when(refreshTokenBlacklistRepository.existsByTokenAndExpiresAtAfter(eq(TOKEN), any(LocalDateTime.class)))
                    .thenReturn(false);

            // when
            boolean result = refreshTokenBlacklistService.isBlacklisted(TOKEN);

            // then
            assertFalse(result);
        }
    }
}
