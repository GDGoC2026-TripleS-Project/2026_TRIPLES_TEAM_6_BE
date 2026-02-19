package com.lastcup.api.domain.notification.service;

import static com.lastcup.api.global.config.AppTimeZone.KST;

import com.lastcup.api.domain.notification.domain.NotificationDispatchLog;
import com.lastcup.api.domain.notification.domain.NotificationType;
import com.lastcup.api.domain.notification.repository.NotificationDispatchLogRepository;
import com.lastcup.api.domain.user.domain.UserNotificationSetting;
import com.lastcup.api.domain.user.repository.UserDeviceRepository;
import com.lastcup.api.domain.user.repository.UserNotificationSettingRepository;
import com.lastcup.api.infrastructure.notification.FcmNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(UserNotificationScheduler.class);

    private static final String RECORD_TITLE = "기록 알림";
    private static final String RECORD_BODY = "오늘의 기록을 남겨보세요.";

    private static final String DAILY_CLOSE_TITLE = "마감 알림";
    private static final String DAILY_CLOSE_BODY = "오늘의 섭취를 마감해 주세요.";

    private final UserNotificationSettingRepository settingRepository;
    private final UserDeviceRepository deviceRepository;
    private final NotificationDispatchLogRepository dispatchLogRepository;
    private final FcmNotificationService fcmNotificationService;

    public UserNotificationScheduler(
            UserNotificationSettingRepository settingRepository,
            UserDeviceRepository deviceRepository,
            NotificationDispatchLogRepository dispatchLogRepository,
            FcmNotificationService fcmNotificationService
    ) {
        this.settingRepository = settingRepository;
        this.deviceRepository = deviceRepository;
        this.dispatchLogRepository = dispatchLogRepository;
        this.fcmNotificationService = fcmNotificationService;
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void sendScheduledNotifications() {
        LocalTime now = LocalTime.now(KST).withSecond(0).withNano(0);
        sendRecordRemind(now);
        sendDailyClose(now);
    }

    private void sendRecordRemind(LocalTime now) {
        List<UserNotificationSetting> settings =
                settingRepository.findAllByIsEnabledTrueAndRecordRemindAt(now);
        processSettings(settings, NotificationType.RECORD_REMIND, RECORD_TITLE, RECORD_BODY);
    }

    private void sendDailyClose(LocalTime now) {
        List<UserNotificationSetting> settings =
                settingRepository.findAllByIsEnabledTrueAndDailyCloseAt(now);
        processSettings(settings, NotificationType.DAILY_CLOSE, DAILY_CLOSE_TITLE, DAILY_CLOSE_BODY);
    }

    private void processSettings(
            List<UserNotificationSetting> settings,
            NotificationType notificationType,
            String title,
            String body
    ) {
        if (settings.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now(KST);
        List<Long> userIds = settings.stream()
                .map(UserNotificationSetting::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Set<Long> sentUserIds = new HashSet<>(
                dispatchLogRepository.findSentUserIdsByTypeAndDateInUsers(notificationType, today, userIds)
        );
        Map<Long, List<String>> tokensByUserId = groupEnabledTokensByUserId(userIds);

        for (UserNotificationSetting setting : settings) {
            Long userId = setting.getUserId();
            if (sentUserIds.contains(userId)) {
                continue;
            }
            List<String> tokens = tokensByUserId.getOrDefault(userId, List.of());
            if (tokens.isEmpty()) {
                continue;
            }

            try {
                fcmNotificationService.sendToTokens(tokens, title, body);
                saveDispatchLog(userId, notificationType, today);
            } catch (Exception e) {
                log.error("Notification send failed. userId={}, type={}", userId, notificationType, e);
            }
        }
    }

    private Map<Long, List<String>> groupEnabledTokensByUserId(Collection<Long> userIds) {
        Map<Long, Set<String>> tokenSetByUserId = new HashMap<>();

        for (UserDeviceRepository.UserDeviceTokenProjection tokenProjection : deviceRepository.findEnabledTokensByUserIds(userIds)) {
            String token = tokenProjection.getFcmToken();
            if (token == null || token.isBlank()) {
                continue;
            }
            tokenSetByUserId
                    .computeIfAbsent(tokenProjection.getUserId(), key -> new HashSet<>())
                    .add(token);
        }

        Map<Long, List<String>> tokensByUserId = new HashMap<>();
        tokenSetByUserId.forEach((userId, tokenSet) -> tokensByUserId.put(userId, new ArrayList<>(tokenSet)));
        return tokensByUserId;
    }


    private void saveDispatchLog(Long userId, NotificationType notificationType, LocalDate today) {
        try {
            dispatchLogRepository.save(NotificationDispatchLog.create(
                    userId,
                    notificationType,
                    today,
                    LocalDateTime.now(KST)
            ));
        } catch (DataIntegrityViolationException e) {
            log.info("Notification dispatch log already exists. userId={}, type={}, date={}",
                    userId,
                    notificationType,
                    today);
        }
    }
}
