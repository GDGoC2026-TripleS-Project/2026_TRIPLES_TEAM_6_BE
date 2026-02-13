package com.lastcup.api.domain.user.service;

import static com.lastcup.api.global.config.AppTimeZone.KST;

import com.lastcup.api.domain.user.domain.User;
import com.lastcup.api.domain.user.domain.UserDevice;
import com.lastcup.api.domain.user.domain.UserPlatform;
import com.lastcup.api.domain.user.dto.response.RegisterDeviceResponse;
import com.lastcup.api.domain.user.repository.UserDeviceRepository;
import com.lastcup.api.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class UserDeviceService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;

    public UserDeviceService(UserRepository userRepository, UserDeviceRepository userDeviceRepository) {
        this.userRepository = userRepository;
        this.userDeviceRepository = userDeviceRepository;
    }

    public RegisterDeviceResponse createOrUpdateDevice(Long userId, String fcmToken, UserPlatform platform) {
        validateToken(fcmToken);

        User user = findUser(userId);
        UserDevice device = findOrCreateByToken(UserDevice.create(user, fcmToken, platform));
        device.updatePlatform(platform);
        device.updateLastSeenAt(LocalDateTime.now(KST));

        userDeviceRepository.save(device);
        return new RegisterDeviceResponse(true);
    }

    private void validateToken(String fcmToken) {
        if (fcmToken != null && !fcmToken.isBlank()) {
            return;
        }
        throw new IllegalArgumentException("fcmToken is blank");
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
    }

    private UserDevice findOrCreateByToken(UserDevice defaultDevice) {
        return userDeviceRepository.findByFcmToken(defaultDevice.getFcmToken())
                .orElse(defaultDevice);
    }
}
