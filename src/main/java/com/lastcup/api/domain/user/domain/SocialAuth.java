package com.lastcup.api.domain.user.domain;

import com.lastcup.api.global.config.BaseTimeEntity;
import com.lastcup.api.infrastructure.oauth.SocialProvider;
import jakarta.persistence.*;

@Entity
@Table(
        name = "social_auth",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_social_auth_user", columnNames = {"user_id"}),
                @UniqueConstraint(name = "uk_social_auth_provider_key", columnNames = {"provider", "provider_user_key"})
        }
)
public class SocialAuth extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_user_key", nullable = false, length = 200)
    private String providerUserKey;

    @Column(length = 320)
    private String email;

    protected SocialAuth() {
    }

    private SocialAuth(Long userId, SocialProvider provider, String providerUserKey, String email) {
        this.userId = userId;
        this.provider = provider;
        this.providerUserKey = providerUserKey;
        this.email = email;
    }

    public static SocialAuth create(Long userId, SocialProvider provider, String providerUserKey, String email) {
        validate(userId, provider, providerUserKey);
        return new SocialAuth(userId, provider, providerUserKey, email);
    }

    private static void validate(Long userId, SocialProvider provider, String providerUserKey) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is null");
        }
        if (provider == null) {
            throw new IllegalArgumentException("provider is null");
        }
        if (providerUserKey == null || providerUserKey.isBlank()) {
            throw new IllegalArgumentException("providerUserKey is blank");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public SocialProvider getProvider() {
        return provider;
    }

    public String getProviderUserKey() {
        return providerUserKey;
    }

    public String getEmail() {
        return email;
    }

    /**
     * 이메일이 아직 없는 경우에만 업데이트한다.
     * Apple 최초 가입 실패 후 재시도 시 이메일을 보충하기 위함.
     */
    public boolean updateEmailIfAbsent(String email) {
        if (this.email != null || email == null || email.isBlank()) {
            return false;
        }
        this.email = email;
        return true;
    }
}
