package com.lastcup.api.domain.user.service;

import com.lastcup.api.domain.user.domain.UserNotificationSetting;
import com.lastcup.api.domain.user.dto.response.NotificationSettingResponse;
import com.lastcup.api.domain.user.dto.response.UpdateNotificationSettingResponse;
import com.lastcup.api.domain.user.repository.UserNotificationSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Service
@Transactional
public class UserNotificationSettingService {

    private static final boolean DEFAULT_ENABLED = true;
    private static final LocalTime DEFAULT_RECORD_REMIND_AT = LocalTime.of(14, 0, 0);
    private static final LocalTime DEFAULT_DAILY_CLOSE_AT = LocalTime.of(21, 0, 0);

    private final UserNotificationSettingRepository repository;

    public UserNotificationSettingService(UserNotificationSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public NotificationSettingResponse findOrCreate(Long userId) {
        UserNotificationSetting setting = repository.findById(userId)
                .orElseGet(() -> createDefault(userId));
        return toResponse(setting);
    }

    public UpdateNotificationSettingResponse update(
            Long userId,
            Boolean isEnabled,
            LocalTime recordRemindAt,
            LocalTime dailyCloseAt
    ) {
        UserNotificationSetting setting = repository.findById(userId)
                .orElseGet(() -> createDefault(userId));

        setting.update(isEnabled, recordRemindAt, dailyCloseAt);
        repository.save(setting);

        return new UpdateNotificationSettingResponse(true);
    }

    private UserNotificationSetting createDefault(Long userId) {
        UserNotificationSetting created = UserNotificationSetting.createDefault(
                userId,
                DEFAULT_ENABLED,
                DEFAULT_RECORD_REMIND_AT,
                DEFAULT_DAILY_CLOSE_AT
        );
        return repository.save(created);
    }

    private NotificationSettingResponse toResponse(UserNotificationSetting setting) {
        return new NotificationSettingResponse(
                setting.isEnabled(),
                setting.getRecordRemindAt(),
                setting.getDailyCloseAt(),
                setting.getCreatedAt(),
                setting.getUpdatedAt()
        );
    }
}
