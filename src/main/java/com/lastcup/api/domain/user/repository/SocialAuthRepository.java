package com.lastcup.api.domain.user.repository;

import com.lastcup.api.domain.user.domain.SocialAuth;
import com.lastcup.api.infrastructure.oauth.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAuthRepository extends JpaRepository<SocialAuth, Long> {
    Optional<SocialAuth> findByProviderAndProviderUserKey(SocialProvider provider, String providerUserKey);

    boolean existsByUserId(Long userId);

    Optional<SocialAuth> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
