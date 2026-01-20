package com.lastcup.api.domain.auth.controller;

import com.lastcup.api.domain.auth.dto.request.LoginRequest;
import com.lastcup.api.domain.auth.dto.request.SignupRequest;
import com.lastcup.api.domain.auth.dto.request.SocialLoginRequest;
import com.lastcup.api.domain.auth.dto.response.*;
import com.lastcup.api.domain.auth.service.AuthService;
import com.lastcup.api.domain.auth.service.SocialLoginService;
import com.lastcup.api.infrastructure.oauth.SocialProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final SocialLoginService socialLoginService;
    private final AuthService authService;

    public AuthController(SocialLoginService socialLoginService, AuthService authService) {
        this.socialLoginService = socialLoginService;
        this.authService = authService;
    }

    @Operation(summary = "아이디 중복 확인", description = "loginId 사용 가능 여부를 반환합니다.")
    @GetMapping("/check-login-id")
    public ApiResponse<AvailabilityResponse> checkLoginId(@RequestParam String loginId) {
        boolean isAvailable = authService.findLoginIdAvailability(loginId);
        return ApiResponse.of(new AvailabilityResponse(isAvailable));
    }

    @Operation(summary = "닉네임 중복 확인", description = "nickname 사용 가능 여부를 반환합니다.")
    @GetMapping("/check-nickname")
    public ApiResponse<AvailabilityResponse> checkNickname(@RequestParam String nickname) {
        boolean isAvailable = authService.findNicknameAvailability(nickname);
        return ApiResponse.of(new AvailabilityResponse(isAvailable));
    }

    @Operation(summary = "로컬 회원가입", description = "아이디/비밀번호 기반으로 가입하고 JWT(access/refresh)를 발급합니다.")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResultResponse> signup(@RequestBody @Valid SignupRequest request) {
        AuthResultResponse response = authService.createSignup(request);
        return ApiResponse.of(response);
    }

    @Operation(summary = "로컬 로그인", description = "아이디/비밀번호를 검증하고 JWT(access/refresh)를 발급합니다.")
    @PostMapping("/login")
    public ApiResponse<AuthResultResponse> login(@RequestBody @Valid LoginRequest request) {
        AuthResultResponse response = authService.createLogin(request);
        return ApiResponse.of(response);
    }

    @Operation(summary = "소셜 로그인", description = "providerAccessToken을 검증하고, 서비스 JWT(access/refresh)를 발급합니다.")
    @PostMapping("/social/{provider}/login")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<AuthResponse> socialLogin(
            @PathVariable SocialProvider provider,
            @RequestBody @Valid SocialLoginRequest request
    ) {
        AuthResponse response = socialLoginService.login(provider, request.providerAccessToken());
        return ApiResponse.of(response);
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token을 사용하여 Access Token을 재발급합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/refresh")
    public ApiResponse<AuthTokensResponse> refresh(HttpServletRequest request) {
        String refreshToken = resolveToken(request);
        AuthTokensResponse tokens = authService.refresh(refreshToken);
        return ApiResponse.of(tokens);
    }

    @Operation(summary = "로그아웃", description = "Refresh Token을 검증하고 로그아웃 처리합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/logout")
    public ApiResponse<Boolean> logout(HttpServletRequest request) {
        String refreshToken = resolveToken(request);
        authService.logout(refreshToken);
        return ApiResponse.of(true);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new IllegalArgumentException("Bearer Token is missing");
    }
}
