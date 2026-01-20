package com.lastcup.api.domain.user.repository;

import com.lastcup.api.domain.user.domain.UserNotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationSettingRepository extends JpaRepository<UserNotificationSetting, Long> {
}
