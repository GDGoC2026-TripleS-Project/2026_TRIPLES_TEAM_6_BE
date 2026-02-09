package com.lastcup.api.domain.notification.repository;

import com.lastcup.api.domain.notification.domain.NotificationDispatchLog;
import com.lastcup.api.domain.notification.domain.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface NotificationDispatchLogRepository extends JpaRepository<NotificationDispatchLog, Long> {
    boolean existsByUserIdAndNotificationTypeAndSentDate(Long userId, NotificationType notificationType, LocalDate sentDate);
}
