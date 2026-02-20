package com.lastcup.api.security;

import com.lastcup.api.global.error.JwtErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-test-secret-key-test-secret-key-1234";

    private JwtProvider jwtProvider;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider("test-issuer", 3_600_000L, 7_200_000L, SECRET);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Access 토큰 생성 시 userId/type/issuer claims가 포함된다")
    void createAccessTokenContainsClaims() {
        String token = jwtProvider.createAccessToken(1L);

        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertEquals(1L, claims.get("userId", Long.class));
        assertEquals("ACCESS", claims.get("type", String.class));
        assertEquals("test-issuer", claims.getIssuer());
    }

    @Test
    @DisplayName("Refresh 토큰은 REFRESH 타입이며 parseRefreshToken이 userId를 반환한다")
    void createAndParseRefreshToken() {
        String token = jwtProvider.createRefreshToken(7L);

        AuthUser authUser = jwtProvider.parseRefreshToken(token);
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertEquals(7L, authUser.userId());
        assertEquals("REFRESH", claims.get("type", String.class));
    }

    @Test
    @DisplayName("요구 토큰 타입이 다르면 JWT_TOKEN_TYPE_MISMATCH")
    void validateTokenTypeMismatch() {
        String refreshToken = jwtProvider.createRefreshToken(3L);

        JwtValidationException exception = assertThrows(JwtValidationException.class,
                () -> jwtProvider.validate(refreshToken, "ACCESS"));

        assertEquals(JwtErrorCode.JWT_TOKEN_TYPE_MISMATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("만료 시각 조회가 null이 아니고 현재 이후 시각이다")
    void expiresAtIsWithinExpectedRange() {
        String accessToken = jwtProvider.createAccessToken(10L);
        String refreshToken = jwtProvider.createRefreshToken(10L);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accessExpiresAt = jwtProvider.getAccessTokenExpiresAt(accessToken);
        LocalDateTime refreshExpiresAt = jwtProvider.getRefreshTokenExpiresAt(refreshToken);

        assertNotNull(accessExpiresAt);
        assertNotNull(refreshExpiresAt);
        assertTrue(accessExpiresAt.isAfter(now.minusSeconds(1)));
        assertTrue(refreshExpiresAt.isAfter(accessExpiresAt));
    }
}
