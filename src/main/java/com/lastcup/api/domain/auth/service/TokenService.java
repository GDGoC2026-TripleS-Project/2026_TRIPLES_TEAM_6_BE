package com.lastcup.api.domain.auth.service;

import com.lastcup.api.domain.auth.dto.response.AuthTokensResponse;
import com.lastcup.api.security.JwtProvider;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final JwtProvider jwtProvider;

    public TokenService(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    public AuthTokensResponse createTokens(Long userId) {
        String accessToken = jwtProvider.createAccessToken(userId);
        String refreshToken = jwtProvider.createRefreshToken(userId);
        return new AuthTokensResponse(accessToken, refreshToken);
    }
}
