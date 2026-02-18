package com.lastcup.api.domain.user.repository;

import com.lastcup.api.domain.user.domain.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    Optional<UserDevice> findByFcmToken(String fcmToken);

    List<UserDevice> findAllByUserIdAndIsEnabledTrue(Long userId);

    @Query("""
            select d.user.id as userId, d.fcmToken as fcmToken
            from UserDevice d
            where d.user.id in :userIds
              and d.isEnabled = true
            """)
    List<UserDeviceTokenProjection> findEnabledTokensByUserIds(@Param("userIds") Collection<Long> userIds);

    interface UserDeviceTokenProjection {
        Long getUserId();

        String getFcmToken();
    }
}
