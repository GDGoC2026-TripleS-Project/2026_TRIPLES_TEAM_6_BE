package com.lastcup.api.domain.auth.service;

import static com.lastcup.api.global.config.AppTimeZone.KST;

import com.lastcup.api.domain.auth.domain.AccessTokenBlacklist;
import com.lastcup.api.domain.auth.repository.AccessTokenBlacklistRepository;
import com.lastcup.api.security.JwtProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AccessTokenBlacklistService {

    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;
    private final JwtProvider jwtProvider;

    public AccessTokenBlacklistService(
            AccessTokenBlacklistRepository accessTokenBlacklistRepository,
            JwtProvider jwtProvider
    ) {
        this.accessTokenBlacklistRepository = accessTokenBlacklistRepository;
        this.jwtProvider = jwtProvider;
    }

    public void blacklist(String accessToken) {
        LocalDateTime expiresAt = jwtProvider.getAccessTokenExpiresAt(accessToken);
        if (accessTokenBlacklistRepository.existsByTokenAndExpiresAtAfter(accessToken, LocalDateTime.now(KST))) {
            return;
        }
        accessTokenBlacklistRepository.save(AccessTokenBlacklist.create(accessToken, expiresAt));
    }

    public boolean isBlacklisted(String accessToken) {
        return accessTokenBlacklistRepository.existsByTokenAndExpiresAtAfter(accessToken, LocalDateTime.now(KST));
    }
}
