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

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    protected User() {
    }

    private User(String nickname, String email, String profileImageUrl) {
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.status = UserStatus.ACTIVE;
    }

    public static User create(String nickname, String email, String profileImageUrl) {
        validateNickname(nickname);
        return new User(nickname, email, profileImageUrl);
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updateNickname(String nickname) {
        validateNickname(nickname);
        this.nickname = nickname;
    }

    /**
     * 이메일이 아직 없는 경우에만 업데이트한다.
     * Apple 로그인은 최초 1회만 이메일을 제공하므로,
     * 첫 가입 시 누락됐다가 이후 클라이언트가 전달하면 보충한다.
     */
    public boolean updateEmailIfAbsent(String email) {
        if (this.email != null || email == null || email.isBlank()) {
            return false;
        }
        this.email = email;
        return true;
    }

    public void delete() {
        this.status = UserStatus.DELETED;
    }

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname is blank");
        }
    }

    public void clearEmail() {
        this.email = null;
    }

    // email은 소셜 로그인 사용자의 경우 null일 수 있음 (카카오 등)
    // 로컬 회원가입은 SignupRequest DTO에서 @NotBlank @Email로 검증

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getEmail() {
        return email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public UserStatus getStatus() {
        return status;
    }
}
