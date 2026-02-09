package com.lastcup.api.domain.auth.repository;

import com.lastcup.api.domain.auth.domain.RefreshTokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface RefreshTokenBlacklistRepository extends JpaRepository<RefreshTokenBlacklist, Long> {

    boolean existsByTokenAndExpiresAtAfter(String token, LocalDateTime now);
}
