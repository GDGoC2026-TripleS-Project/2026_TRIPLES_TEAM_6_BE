package com.lastcup.api.domain.auth.service;

import com.lastcup.api.domain.auth.dto.request.LoginRequest;
import com.lastcup.api.domain.auth.dto.request.SignupRequest;
import com.lastcup.api.domain.auth.dto.response.AuthResultResponse;
import com.lastcup.api.domain.auth.dto.response.AuthTokensResponse;
import com.lastcup.api.domain.auth.dto.response.UserSummaryResponse;
import com.lastcup.api.domain.user.domain.LocalAuth;
import com.lastcup.api.domain.user.domain.User;
import com.lastcup.api.domain.user.domain.UserStatus;
import com.lastcup.api.domain.user.repository.LocalAuthRepository;
import com.lastcup.api.domain.user.repository.UserRepository;
import com.lastcup.api.security.AuthUser;
import com.lastcup.api.security.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final JwtProvider jwtProvider;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final LocalAuthRepository localAuthRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            JwtProvider jwtProvider,
            TokenService tokenService,
            UserRepository userRepository,
            LocalAuthRepository localAuthRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.jwtProvider = jwtProvider;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.localAuthRepository = localAuthRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResultResponse createSignup(SignupRequest request) {
        validateSignup(request);

        User user = userRepository.save(User.create(request.nickname(), null));
        localAuthRepository.save(createLocalAuth(user.getId(), request));

        AuthTokensResponse tokens = tokenService.createTokens(user.getId());
        return new AuthResultResponse(new UserSummaryResponse(user.getId(), user.getNickname()), tokens);
    }

    @Transactional(readOnly = true)
    public AuthResultResponse createLogin(LoginRequest request) {
        LocalAuth localAuth = localAuthRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new IllegalArgumentException("loginId not found"));

        validatePassword(request.password(), localAuth.getPasswordHash());

        User user = userRepository.findById(localAuth.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        validateUserStatus(user);

        AuthTokensResponse tokens = tokenService.createTokens(user.getId());
        return new AuthResultResponse(new UserSummaryResponse(user.getId(), user.getNickname()), tokens);
    }

    public boolean findLoginIdAvailability(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("loginId is blank");
        }
        return !localAuthRepository.existsByLoginId(loginId);
    }

    public boolean findNicknameAvailability(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname is blank");
        }
        return !userRepository.existsByNickname(nickname);
    }

    public AuthTokensResponse refresh(String refreshToken) {
        jwtProvider.validate(refreshToken, "REFRESH");

        AuthUser authUser = jwtProvider.parse(refreshToken);

        return tokenService.createTokens(authUser.userId());
    }

    public void logout(String refreshToken) {
        jwtProvider.validate(refreshToken, "REFRESH");
        // 검증만 성공하면 로그아웃
    }

    private void validateSignup(SignupRequest request) {
        if (localAuthRepository.existsByLoginId(request.loginId())) {
            throw new IllegalArgumentException("loginId already exists");
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new IllegalArgumentException("nickname already exists");
        }
    }

    private LocalAuth createLocalAuth(Long userId, SignupRequest request) {
        String passwordHash = passwordEncoder.encode(request.password());
        return LocalAuth.create(userId, request.loginId(), passwordHash);
    }

    private void validatePassword(String rawPassword, String passwordHash) {
        if (passwordEncoder.matches(rawPassword, passwordHash)) {
            return;
        }
        throw new IllegalArgumentException("password mismatch");
    }

    private void validateUserStatus(User user) {
        if (user.getStatus() == UserStatus.ACTIVE) {
            return;
        }
        throw new IllegalArgumentException("user is not active");
    }
}
