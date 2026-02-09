package com.lastcup.api.domain.auth.domain;

import com.lastcup.api.global.config.BaseTimeEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "refresh_token_blacklist",
        indexes = {
                @Index(name = "idx_refresh_token_blacklist_token", columnList = "token"),
                @Index(name = "idx_refresh_token_blacklist_expires_at", columnList = "expires_at")
        }
)
public class RefreshTokenBlacklist extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 600)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected RefreshTokenBlacklist() {
    }

    private RefreshTokenBlacklist(String token, LocalDateTime expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public static RefreshTokenBlacklist create(String token, LocalDateTime expiresAt) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token is blank");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt is null");
        }
        return new RefreshTokenBlacklist(token, expiresAt);
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
