package com.lastcup.api.domain.user.domain;

import com.lastcup.api.global.config.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    protected User() {
    }

    private User(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.status = UserStatus.ACTIVE;
    }

    public static User create(String nickname, String profileImageUrl) {
        validateNickname(nickname);
        return new User(nickname, profileImageUrl);
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updateNickname(String nickname) {
        validateNickname(nickname);
        this.nickname = nickname;
    }

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname is blank");
        }
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public UserStatus getStatus() {
        return status;
    }
}
