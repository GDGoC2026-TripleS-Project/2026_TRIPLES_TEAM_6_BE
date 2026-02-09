package com.lastcup.api.domain.user.domain;

import com.lastcup.api.global.config.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;

@Entity
@Table(name = "user_notification_settings")
public class UserNotificationSetting extends BaseTimeEntity {

    @Id
    private Long userId;

    @Column(nullable = false)
    private boolean isEnabled;

    private LocalTime recordRemindAt;

    private LocalTime dailyCloseAt;

    protected UserNotificationSetting() {
    }

    private UserNotificationSetting(Long userId, boolean isEnabled, LocalTime recordRemindAt, LocalTime dailyCloseAt) {
        this.userId = userId;
        this.isEnabled = isEnabled;
        this.recordRemindAt = recordRemindAt;
        this.dailyCloseAt = dailyCloseAt;
    }

    public static UserNotificationSetting createDefault(
            Long userId,
            boolean isEnabled,
            LocalTime recordRemindAt,
            LocalTime dailyCloseAt
    ) {
        validateUserId(userId);
        return new UserNotificationSetting(userId, isEnabled, recordRemindAt, dailyCloseAt);
    }

    public void update(Boolean isEnabled, LocalTime recordRemindAt, LocalTime dailyCloseAt) {
        updateEnabled(isEnabled);
        updateRecordRemindAt(recordRemindAt);
        updateDailyCloseAt(dailyCloseAt);
    }

    private void updateEnabled(Boolean isEnabled) {
        if (isEnabled == null) {
            return;
        }
        this.isEnabled = isEnabled;
    }

    private void updateRecordRemindAt(LocalTime recordRemindAt) {
        if (recordRemindAt == null) {
            return;
        }
        this.recordRemindAt = recordRemindAt;
    }

    private void updateDailyCloseAt(LocalTime dailyCloseAt) {
        if (dailyCloseAt == null) {
            return;
        }
        this.dailyCloseAt = dailyCloseAt;
    }

    private static void validateUserId(Long userId) {
        if (userId != null && userId > 0) {
            return;
        }
        throw new IllegalArgumentException("userId is invalid");
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalTime getRecordRemindAt() {
        return recordRemindAt;
    }

    public LocalTime getDailyCloseAt() {
        return dailyCloseAt;
    }
}
