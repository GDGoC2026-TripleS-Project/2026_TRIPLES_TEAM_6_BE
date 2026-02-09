package com.lastcup.api.domain.user.repository;

import com.lastcup.api.domain.user.domain.UserNotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;

public interface UserNotificationSettingRepository extends JpaRepository<UserNotificationSetting, Long> {
    List<UserNotificationSetting> findAllByIsEnabledTrueAndRecordRemindAt(LocalTime recordRemindAt);

    List<UserNotificationSetting> findAllByIsEnabledTrueAndDailyCloseAt(LocalTime dailyCloseAt);
}
