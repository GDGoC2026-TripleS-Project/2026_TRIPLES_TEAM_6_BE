package com.lastcup.api.security;

import com.lastcup.api.global.error.JwtErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class JwtProvider {

    private final String issuer;
    private final long accessTtlMs;
    private final long refreshTtlMs;
    private final SecretKey key;

    public JwtProvider(
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.access-token-validity}") long accessTtlMs,
            @Value("${jwt.refresh-token-validity}") long refreshTtlMs,
            @Value("${jwt.secret}") String secret
    ) {
        this.issuer = issuer;
        this.accessTtlMs = accessTtlMs;
        this.refreshTtlMs = refreshTtlMs;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId) {
        return createToken(userId, accessTtlMs, "ACCESS");
    }

    public String createRefreshToken(Long userId) {
        return createToken(userId, refreshTtlMs, "REFRESH");
    }

    public void validate(String token, String requiredType) {
        Claims claims = parseClaims(token, requiredType);
        String type = claims.get("type", String.class);
        if (requiredType.equals(type)) {
            return;
        }
        throw new JwtValidationException(JwtErrorCode.JWT_TOKEN_TYPE_MISMATCH);
    }

    public AuthUser parseAccessToken(String token) {
        Claims claims = parseClaims(token, "ACCESS");
        Long userId = claims.get("userId", Long.class);
        return new AuthUser(userId);
    }

    public AuthUser parseRefreshToken(String token) {
        Claims claims = parseClaims(token, "REFRESH");
        Long userId = claims.get("userId", Long.class);
        return new AuthUser(userId);
    }

    public LocalDateTime getAccessTokenExpiresAt(String token) {
        Claims claims = parseClaims(token, "ACCESS");
        return LocalDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneId.systemDefault());
    }

    public LocalDateTime getRefreshTokenExpiresAt(String token) {
        Claims claims = parseClaims(token, "REFRESH");
        return LocalDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneId.systemDefault());
    }

    private String createToken(Long userId, long ttlMs, String type) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(ttlMs);

        return Jwts.builder()
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim("userId", userId)
                .claim("type", type)
                .signWith(key)
                .compact();
    }

    private Claims parseClaims(String token, String tokenType) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new JwtValidationException(resolveInvalidTokenCode(tokenType));
        }
    }

    private JwtErrorCode resolveInvalidTokenCode(String tokenType) {
        if ("REFRESH".equals(tokenType)) {
            return JwtErrorCode.JWT_REFRESH_INVALID;
        }
        return JwtErrorCode.JWT_ACCESS_INVALID;
    }
}
