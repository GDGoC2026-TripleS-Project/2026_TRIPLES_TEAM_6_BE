package com.lastcup.api.domain.user.repository;

import com.lastcup.api.domain.user.domain.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    Optional<UserDevice> findByFcmToken(String fcmToken);

    List<UserDevice> findAllByUserIdAndIsEnabledTrue(Long userId);
}
