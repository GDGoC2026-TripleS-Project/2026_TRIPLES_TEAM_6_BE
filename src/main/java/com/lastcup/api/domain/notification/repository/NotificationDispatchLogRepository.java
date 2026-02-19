package com.lastcup.api.domain.notification.repository;

import com.lastcup.api.domain.notification.domain.NotificationDispatchLog;
import com.lastcup.api.domain.notification.domain.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface NotificationDispatchLogRepository extends JpaRepository<NotificationDispatchLog, Long> {
    boolean existsByUserIdAndNotificationTypeAndSentDate(Long userId, NotificationType notificationType, LocalDate sentDate);

    @Query("""
            select distinct l.userId
            from NotificationDispatchLog l
            where l.notificationType = :notificationType
              and l.sentDate = :sentDate
              and l.userId in :userIds
            """)
    List<Long> findSentUserIdsByTypeAndDateInUsers(
            @Param("notificationType") NotificationType notificationType,
            @Param("sentDate") LocalDate sentDate,
            @Param("userIds") Collection<Long> userIds
    );
}
