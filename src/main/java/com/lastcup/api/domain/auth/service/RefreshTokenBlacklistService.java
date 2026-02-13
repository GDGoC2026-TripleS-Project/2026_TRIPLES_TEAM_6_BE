package com.lastcup.api.domain.auth.service;

import static com.lastcup.api.global.config.AppTimeZone.KST;

import com.lastcup.api.domain.auth.domain.RefreshTokenBlacklist;
import com.lastcup.api.domain.auth.repository.RefreshTokenBlacklistRepository;
import com.lastcup.api.security.JwtProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RefreshTokenBlacklistService {

    private final RefreshTokenBlacklistRepository refreshTokenBlacklistRepository;
    private final JwtProvider jwtProvider;

    public RefreshTokenBlacklistService(
            RefreshTokenBlacklistRepository refreshTokenBlacklistRepository,
            JwtProvider jwtProvider
    ) {
        this.refreshTokenBlacklistRepository = refreshTokenBlacklistRepository;
        this.jwtProvider = jwtProvider;
    }

    public void blacklist(String refreshToken) {
        LocalDateTime expiresAt = jwtProvider.getRefreshTokenExpiresAt(refreshToken);
        if (refreshTokenBlacklistRepository.existsByTokenAndExpiresAtAfter(refreshToken, LocalDateTime.now(KST))) {
            return;
        }
        refreshTokenBlacklistRepository.save(RefreshTokenBlacklist.create(refreshToken, expiresAt));
    }

    public boolean isBlacklisted(String refreshToken) {
        return refreshTokenBlacklistRepository.existsByTokenAndExpiresAtAfter(refreshToken, LocalDateTime.now(KST));
    }
}
