package com.lastcup.api.domain.auth.service;

import com.lastcup.api.domain.auth.domain.AccessTokenBlacklist;
import com.lastcup.api.domain.auth.repository.AccessTokenBlacklistRepository;
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
 * AccessTokenBlacklistService 단위 테스트.
 * 로그아웃 시 access token 블랙리스트 등록/조회 로직을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AccessTokenBlacklistServiceTest {

    @Mock private AccessTokenBlacklistRepository accessTokenBlacklistRepository;
    @Mock private JwtProvider jwtProvider;

    @InjectMocks
    private AccessTokenBlacklistService accessTokenBlacklistService;

    private static final String TOKEN = "test-access-token";
    private static final LocalDateTime FUTURE_EXPIRY = LocalDateTime.of(2099, 12, 31, 23, 59);

    // 1. blacklist — 토큰 블랙리스트 등록
    // 흐름: getExpiresAt → 이미 존재하면 스킵 → 없으면 save

    @Nested
    @DisplayName("토큰 블랙리스트 등록 (blacklist)")
    class Blacklist {

        @Test
        @DisplayName("정상 등록 — 새 토큰 블랙리스트에 저장")
        void blacklistNewToken() {
            // given — JwtProvider에서 만료 시각 추출, 아직 블랙리스트에 없음
            when(jwtProvider.getAccessTokenExpiresAt(TOKEN)).thenReturn(FUTURE_EXPIRY);
            when(accessTokenBlacklistRepository.existsByTokenAndExpiresAtAfter(eq(TOKEN), any(LocalDateTime.class)))
                    .thenReturn(false);

            // when
            accessTokenBlacklistService.blacklist(TOKEN);

            // then — save가 호출되어야 함
            verify(accessTokenBlacklistRepository).save(any(AccessTokenBlacklist.class));
        }

        @Test
        @DisplayName("이미 블랙리스트에 존재 — 저장 스킵 (멱등성)")
        void blacklistAlreadyExists() {
            // given — 이미 블랙리스트에 있으면 early return
            when(jwtProvider.getAccessTokenExpiresAt(TOKEN)).thenReturn(FUTURE_EXPIRY);
            when(accessTokenBlacklistRepository.existsByTokenAndExpiresAtAfter(eq(TOKEN), any(LocalDateTime.class)))
                    .thenReturn(true);

            // when
            accessTokenBlacklistService.blacklist(TOKEN);

            // then — save가 호출되면 안 됨
            verify(accessTokenBlacklistRepository, never()).save(any());
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
            when(accessTokenBlacklistRepository.existsByTokenAndExpiresAtAfter(eq(TOKEN), any(LocalDateTime.class)))
                    .thenReturn(true);

            // when
            boolean result = accessTokenBlacklistService.isBlacklisted(TOKEN);

            // then
            assertTrue(result);
        }

        @Test
        @DisplayName("블랙리스트에 없음 — false 반환")
        void isBlacklistedFalse() {
            // given
            when(accessTokenBlacklistRepository.existsByTokenAndExpiresAtAfter(eq(TOKEN), any(LocalDateTime.class)))
                    .thenReturn(false);

            // when
            boolean result = accessTokenBlacklistService.isBlacklisted(TOKEN);

            // then
            assertFalse(result);
        }
    }
}
