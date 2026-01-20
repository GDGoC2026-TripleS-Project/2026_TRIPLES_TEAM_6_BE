package com.lastcup.api.domain.user.repository;

import com.lastcup.api.domain.user.domain.LocalAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalAuthRepository extends JpaRepository<LocalAuth, Long> {

    boolean existsByLoginId(String loginId);

    Optional<LocalAuth> findByLoginId(String loginId);
}
