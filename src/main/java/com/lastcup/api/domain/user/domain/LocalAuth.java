package com.lastcup.api.domain.user.domain;

import com.lastcup.api.global.config.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(
        name = "local_auth",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_local_auth_login_id", columnNames = {"login_id"})
        }
)
public class LocalAuth extends BaseTimeEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "login_id", nullable = false, length = 100)
    private String loginId;

    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;

    protected LocalAuth() {
    }

    private LocalAuth(Long userId, String loginId, String passwordHash) {
        this.userId = userId;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
    }

    public static LocalAuth create(Long userId, String loginId, String passwordHash) {
        validate(userId, loginId, passwordHash);
        return new LocalAuth(userId, loginId, passwordHash);
    }

    private static void validate(Long userId, String loginId, String passwordHash) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("loginId is blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash is blank");
        }
    }

    public Long getUserId() {
        return userId;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
