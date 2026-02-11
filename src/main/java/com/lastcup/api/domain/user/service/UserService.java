package com.lastcup.api.domain.user.service;

import com.lastcup.api.domain.user.domain.SocialAuth;
import com.lastcup.api.domain.user.domain.User;
import com.lastcup.api.domain.user.domain.UserStatus;
import com.lastcup.api.domain.user.dto.response.DeleteUserResponse;
import com.lastcup.api.domain.user.dto.response.LoginType;
import com.lastcup.api.domain.user.dto.response.ProfileImageResponse;
import com.lastcup.api.domain.user.dto.response.UpdateNicknameResponse;
import com.lastcup.api.domain.user.dto.response.UserMeResponse;
import com.lastcup.api.domain.user.repository.LocalAuthRepository;
import com.lastcup.api.domain.user.repository.SocialAuthRepository;
import com.lastcup.api.domain.user.repository.UserRepository;
import com.lastcup.api.infrastructure.oauth.SocialProvider;
import com.lastcup.api.infrastructure.storage.StorageService;
import com.lastcup.api.infrastructure.storage.UploadResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final LocalAuthRepository localAuthRepository;
    private final SocialAuthRepository socialAuthRepository;
    private final StorageService storageService;

    public UserService(
            UserRepository userRepository,
            LocalAuthRepository localAuthRepository,
            SocialAuthRepository socialAuthRepository,
            StorageService storageService
    ) {
        this.userRepository = userRepository;
        this.localAuthRepository = localAuthRepository;
        this.socialAuthRepository = socialAuthRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public UserMeResponse findMe(Long userId) {
        User user = findActiveUser(userId);
        return mapToMeResponse(user);
    }

    public UpdateNicknameResponse updateNickname(Long userId, String nickname) {
        User user = findActiveUser(userId);
        validateNicknameAvailable(user, nickname);
        user.updateNickname(nickname);
        return new UpdateNicknameResponse(user.getId(), user.getNickname());
    }

    public ProfileImageResponse updateProfileImage(Long userId, MultipartFile file) {
        User user = findActiveUser(userId);
        UploadResult uploaded = storageService.uploadProfileImage(userId, file);
        user.updateProfileImage(uploaded.url());
        return new ProfileImageResponse(user.getProfileImageUrl());
    }

    public DeleteUserResponse deleteMe(Long userId) {
        User user = findActiveUser(userId);
        user.delete();
        user.clearEmail();
        socialAuthRepository.deleteByUserId(userId);
        return new DeleteUserResponse(UserStatus.DELETED);
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        validateActive(user);
        return user;
    }

    private void validateActive(User user) {
        if (user.getStatus() == UserStatus.ACTIVE) {
            return;
        }
        throw new IllegalArgumentException("user is not active");
    }

    private void validateNicknameAvailable(User user, String nickname) {
        if (user.getNickname().equals(nickname)) {
            return;
        }
        if (!userRepository.existsByNickname(nickname)) {
            return;
        }
        throw new IllegalArgumentException("nickname already exists");
    }

    private UserMeResponse mapToMeResponse(User user) {
        LoginType loginType = resolveLoginType(user.getId());
        SocialProvider socialProvider = resolveSocialProvider(user.getId(), loginType);

        return new UserMeResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                user.getProfileImageUrl(),
                loginType,
                socialProvider,
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private LoginType resolveLoginType(Long userId) {
        if (localAuthRepository.existsById(userId)) {
            return LoginType.LOCAL;
        }
        if (socialAuthRepository.existsByUserId(userId)) {
            return LoginType.SOCIAL;
        }
        throw new IllegalStateException("auth method not found");
    }

    private SocialProvider resolveSocialProvider(Long userId, LoginType loginType) {
        if (loginType == LoginType.LOCAL) {
            return null;
        }

        SocialAuth socialAuth = socialAuthRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("social auth not found"));
        return socialAuth.getProvider();
    }
}
