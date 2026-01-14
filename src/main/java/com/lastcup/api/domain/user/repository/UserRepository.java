package com.lastcup.api.domain.user.repository;

import com.lastcup.api.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByNickname(String nickname);
}
