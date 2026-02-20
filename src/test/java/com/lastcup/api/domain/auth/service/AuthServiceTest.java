package com.lastcup.api.domain.auth.service;

import com.lastcup.api.domain.auth.dto.request.LoginRequest;
import com.lastcup.api.domain.auth.dto.request.SignupRequest;
import com.lastcup.api.domain.auth.dto.response.AuthResultResponse;
import com.lastcup.api.domain.auth.dto.response.AuthTokensResponse;
import com.lastcup.api.domain.user.domain.LocalAuth;
import com.lastcup.api.domain.user.domain.User;
import com.lastcup.api.domain.user.domain.UserStatus;
import com.lastcup.api.domain.user.repository.LocalAuthRepository;
import com.lastcup.api.domain.user.repository.UserRepository;
import com.lastcup.api.domain.user.service.UserNotificationSettingService;
import com.lastcup.api.global.error.JwtErrorCode;
import com.lastcup.api.security.AuthUser;
import com.lastcup.api.security.JwtProvider;
import com.lastcup.api.security.JwtValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private JwtProvider jwtProvider;
    @Mock private TokenService tokenService;
    @Mock private AccessTokenBlacklistService accessTokenBlacklistService;
    @Mock private RefreshTokenBlacklistService refreshTokenBlacklistService;
    @Mock private UserRepository userRepository;
    @Mock private LocalAuthRepository localAuthRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserNotificationSettingService notificationSettingService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("signup 성공 시 사용자/로컬인증 저장 후 토큰을 반환한다")
    void createSignupSuccess() {
        SignupRequest request = new SignupRequest("login123", "user@test.com", "password123", "tester");
        User savedUser = User.create("tester", "user@test.com", null);
        ReflectionTestUtils.setField(savedUser, "id", 100L);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenService.createTokens(100L)).thenReturn(new AuthTokensResponse("access", "refresh"));
        when(passwordEncoder.encode("password123")).thenReturn("encoded");

        AuthResultResponse response = authService.createSignup(request);

        assertEquals(100L, response.user().id());
        assertEquals("tester", response.user().nickname());
        assertEquals("access", response.tokens().accessToken());
        verify(localAuthRepository).save(any(LocalAuth.class));
        verify(notificationSettingService).ensureDefaultExists(100L);
        verify(tokenService).createTokens(100L);
    }

    @Test
    @DisplayName("signup 중복 loginId면 예외 메시지가 고정된다")
    void createSignupDuplicateLoginId() {
        SignupRequest request = new SignupRequest("login123", "user@test.com", "password123", "tester");
        when(localAuthRepository.existsByLoginId("login123")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.createSignup(request));

        assertEquals("loginId already exists", ex.getMessage());
    }

    @Test
    @DisplayName("login 실패 - 비밀번호 불일치")
    void createLoginPasswordMismatch() {
        LoginRequest request = new LoginRequest("login123", "wrongpass");
        LocalAuth localAuth = LocalAuth.create(5L, "login123", "hash");
        when(localAuthRepository.findByLoginId("login123")).thenReturn(Optional.of(localAuth));
        when(passwordEncoder.matches("wrongpass", "hash")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.createLogin(request));

        assertEquals("password mismatch", ex.getMessage());
    }

    @Test
    @DisplayName("login 성공")
    void createLoginSuccess() {
        LoginRequest request = new LoginRequest("login123", "password123");
        LocalAuth localAuth = LocalAuth.create(5L, "login123", "hash");
        User user = User.create("nick", "u@test.com", null);
        ReflectionTestUtils.setField(user, "id", 5L);
        ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);

        when(localAuthRepository.findByLoginId("login123")).thenReturn(Optional.of(localAuth));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(tokenService.createTokens(5L)).thenReturn(new AuthTokensResponse("access", "refresh"));

        AuthResultResponse response = authService.createLogin(request);

        assertEquals(5L, response.user().id());
        assertEquals("access", response.tokens().accessToken());
    }

    @Test
    @DisplayName("refresh 토큰이 블랙리스트면 JWT_REFRESH_INVALID")
    void refreshWithBlacklistedToken() {
        String refreshToken = "refresh";
        doNothing().when(jwtProvider).validate(refreshToken, "REFRESH");
        when(refreshTokenBlacklistService.isBlacklisted(refreshToken)).thenReturn(true);

        JwtValidationException ex = assertThrows(JwtValidationException.class,
                () -> authService.refresh(refreshToken));

        assertEquals(JwtErrorCode.JWT_REFRESH_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("logout 시 access/refresh 모두 validate 후 블랙리스트 처리")
    void logoutCallsBlacklist() {
        authService.logout("access", "refresh");

        verify(jwtProvider).validate("access", "ACCESS");
        verify(jwtProvider).validate("refresh", "REFRESH");
        verify(accessTokenBlacklistService).blacklist("access");
        verify(refreshTokenBlacklistService).blacklist("refresh");
    }
}
