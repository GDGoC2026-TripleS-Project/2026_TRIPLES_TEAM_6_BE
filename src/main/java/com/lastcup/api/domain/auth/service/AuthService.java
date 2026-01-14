package com.lastcup.api.domain.auth.service;

import com.lastcup.api.domain.auth.dto.response.AuthTokensResponse;
import com.lastcup.api.security.AuthUser;
import com.lastcup.api.security.JwtProvider;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtProvider jwtProvider;
    private final TokenService tokenService;

    public AuthService(JwtProvider jwtProvider, TokenService tokenService) {
        this.jwtProvider = jwtProvider;
        this.tokenService = tokenService;
    }

    public AuthTokensResponse refresh(String refreshToken) {
        jwtProvider.validate(refreshToken, "REFRESH");

        AuthUser authUser = jwtProvider.parse(refreshToken);

        return tokenService.createTokens(authUser.userId());
    }

    public void logout(String refreshToken) {
        jwtProvider.validate(refreshToken, "REFRESH");
        //검증만 성공하면 로그아웃
    }
}