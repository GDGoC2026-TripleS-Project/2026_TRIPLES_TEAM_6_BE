package com.lastcup.api.domain.notification.domain;

import com.lastcup.api.global.config.BaseTimeEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_dispatch_logs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_dispatch",
                        columnNames = {"user_id", "notification_type", "sent_date"}
                )
        }
)
public class NotificationDispatchLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType notificationType;

    @Column(name = "sent_date", nullable = false)
    private LocalDate sentDate;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    protected NotificationDispatchLog() {
    }

    private NotificationDispatchLog(Long userId, NotificationType notificationType, LocalDate sentDate, LocalDateTime sentAt) {
        this.userId = userId;
        this.notificationType = notificationType;
        this.sentDate = sentDate;
        this.sentAt = sentAt;
    }

    public static NotificationDispatchLog create(Long userId, NotificationType notificationType, LocalDate sentDate, LocalDateTime sentAt) {
        return new NotificationDispatchLog(userId, notificationType, sentDate, sentAt);
    }
}
