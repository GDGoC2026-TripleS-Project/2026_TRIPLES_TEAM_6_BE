package com.lastcup.api.domain.auth.repository;

import com.lastcup.api.domain.auth.domain.AccessTokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AccessTokenBlacklistRepository extends JpaRepository<AccessTokenBlacklist, Long> {

    boolean existsByTokenAndExpiresAtAfter(String token, LocalDateTime now);
}
