package com.lastcup.api.domain.user.domain;

import com.lastcup.api.global.config.BaseTimeEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_devices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_devices_fcm_token", columnNames = {"fcm_token"})
        }
)
public class UserDevice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "fcm_token", nullable = false, length = 500)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserPlatform platform;

    @Column(nullable = false)
    private boolean isEnabled;

    private LocalDateTime lastSeenAt;

    protected UserDevice() {
    }

    private UserDevice(User user, String fcmToken, UserPlatform platform) {
        this.user = user;
        this.fcmToken = fcmToken;
        this.platform = platform;
        this.isEnabled = true;
        this.lastSeenAt = null;
    }

    public static UserDevice create(User user, String fcmToken, UserPlatform platform) {
        validateUser(user);
        validateToken(fcmToken);
        return new UserDevice(user, fcmToken, platform);
    }

    public void updateLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public void updatePlatform(UserPlatform platform) {
        if (platform == null) {
            return;
        }
        this.platform = platform;
    }

    private static void validateUser(User user) {
        if (user != null) {
            return;
        }
        throw new IllegalArgumentException("user is null");
    }

    private static void validateToken(String fcmToken) {
        if (fcmToken != null && !fcmToken.isBlank()) {
            return;
        }
        throw new IllegalArgumentException("fcmToken is blank");
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public UserPlatform getPlatform() {
        return platform;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }
}
