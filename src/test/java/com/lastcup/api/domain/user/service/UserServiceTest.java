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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UserService 단위 테스트.
 * 내 정보 조회, 닉네임 변경, 프로필 이미지, 회원 탈퇴 로직을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private LocalAuthRepository localAuthRepository;
    @Mock private SocialAuthRepository socialAuthRepository;
    @Mock private StorageService storageService;

    @InjectMocks
    private UserService userService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.create("기존닉네임", "user@test.com", "https://img.url/profile.jpg");
        ReflectionTestUtils.setField(activeUser, "id", 1L);
    }

    // ── 테스트 픽스처 헬퍼 ──

    private void stubActiveUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
    }

    private User createDeletedUser(Long id) {
        User user = User.create("탈퇴유저", "del@test.com", null);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "status", UserStatus.DELETED);
        return user;
    }

    // 1. findMe — 내 정보 조회
    // resolveLoginType()으로 LOCAL/SOCIAL을 판별하고,
    // SOCIAL이면 resolveSocialProvider()로 제공자(GOOGLE/APPLE/KAKAO)를 조회한다.

    @Nested
    @DisplayName("내 정보 조회 (findMe)")
    class FindMe {

        @Test
        @DisplayName("LOCAL 유저 조회 성공 — socialProvider는 null")
        void findMeLocalUser() {
            // given — LocalAuth가 존재하는 유저 (일반 회원가입)
            stubActiveUser();
            when(localAuthRepository.existsById(1L)).thenReturn(true);

            // when
            UserMeResponse response = userService.findMe(1L);

            // then — LOCAL 유저는 socialProvider가 null
            assertEquals(1L, response.id());
            assertEquals("기존닉네임", response.nickname());
            assertEquals("user@test.com", response.email());
            assertEquals(LoginType.LOCAL, response.loginType());
            assertNull(response.socialProvider());
        }

        @Test
        @DisplayName("SOCIAL 유저 조회 성공 — socialProvider 포함")
        void findMeSocialUser() {
            // given — LocalAuth 없고 SocialAuth가 존재하는 유저
            stubActiveUser();
            when(localAuthRepository.existsById(1L)).thenReturn(false);
            when(socialAuthRepository.existsByUserId(1L)).thenReturn(true);

            SocialAuth socialAuth = SocialAuth.create(1L, SocialProvider.GOOGLE, "google-key", "user@test.com");
            when(socialAuthRepository.findByUserId(1L)).thenReturn(Optional.of(socialAuth));

            // when
            UserMeResponse response = userService.findMe(1L);

            // then
            assertEquals(LoginType.SOCIAL, response.loginType());
            assertEquals(SocialProvider.GOOGLE, response.socialProvider());
        }

        @Test
        @DisplayName("존재하지 않는 유저 조회 시 예외")
        void findMeUserNotFound() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> userService.findMe(999L));

            assertEquals("user not found", ex.getMessage());
        }

        @Test
        @DisplayName("DELETED 상태 유저 조회 시 예외")
        void findMeDeletedUser() {
            // given — 탈퇴 처리된 유저는 조회 불가
            User deletedUser = createDeletedUser(2L);
            when(userRepository.findById(2L)).thenReturn(Optional.of(deletedUser));

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> userService.findMe(2L));

            assertEquals("user is not active", ex.getMessage());
        }

        @Test
        @DisplayName("LocalAuth도 SocialAuth도 없는 유저 — 예외")
        void findMeNoAuthMethod() {
            // given — 정상적인 상황에서는 발생하지 않는 데이터 정합성 오류
            // 회원가입 시 반드시 LocalAuth 또는 SocialAuth가 생성되어야 하기 때문
            stubActiveUser();
            when(localAuthRepository.existsById(1L)).thenReturn(false);
            when(socialAuthRepository.existsByUserId(1L)).thenReturn(false);

            // when & then — IllegalStateException: 시스템 오류
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> userService.findMe(1L));

            assertEquals("auth method not found", ex.getMessage());
        }
    }

    // 2. updateNickname — 닉네임 변경

    @Nested
    @DisplayName("닉네임 변경 (updateNickname)")
    class UpdateNickname {

        @Test
        @DisplayName("새 닉네임으로 정상 변경")
        void updateNicknameSuccess() {
            // given
            stubActiveUser();
            when(userRepository.existsByNickname("새닉네임")).thenReturn(false);

            // when
            UpdateNicknameResponse response = userService.updateNickname(1L, "새닉네임");

            // then — 응답값과 실제 엔티티 상태 모두 검증 (상태 검증)
            assertEquals(1L, response.id());
            assertEquals("새닉네임", response.nickname());
            assertEquals("새닉네임", activeUser.getNickname());
        }

        @Test
        @DisplayName("현재와 동일한 닉네임 — 중복 체크 스킵, 정상 반환")
        void updateNicknameSameAsCurrent() {
            // given — 동일 닉네임이면 validateNicknameAvailable()에서 early return
            stubActiveUser();

            // when
            UpdateNicknameResponse response = userService.updateNickname(1L, "기존닉네임");

            // then — DB 중복 체크가 호출되지 않아야 한다 (불필요한 쿼리 방지)
            assertEquals("기존닉네임", response.nickname());
            verify(userRepository, never()).existsByNickname(any());
        }

        @Test
        @DisplayName("이미 사용 중인 닉네임으로 변경 시 예외")
        void updateNicknameDuplicate() {
            // given
            stubActiveUser();
            when(userRepository.existsByNickname("중복닉네임")).thenReturn(true);

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> userService.updateNickname(1L, "중복닉네임"));

            assertEquals("nickname already exists", ex.getMessage());
        }
    }

    // 3. updateProfileImage — 프로필 이미지 변경

    @Nested
    @DisplayName("프로필 이미지 변경 (updateProfileImage)")
    class UpdateProfileImage {

        @Test
        @DisplayName("이미지 업로드 성공 — URL 반환")
        void updateProfileImageSuccess() {
            // given
            stubActiveUser();
            MultipartFile file = mock(MultipartFile.class);
            UploadResult result = new UploadResult("users/1/profile/img.jpg", "https://s3.url/img.jpg", 1024);
            when(storageService.uploadProfileImage(1L, file)).thenReturn(result);

            // when
            ProfileImageResponse response = userService.updateProfileImage(1L, file);

            // then — 응답값과 엔티티 상태 모두 검증
            assertEquals("https://s3.url/img.jpg", response.profileImageUrl());
            assertEquals("https://s3.url/img.jpg", activeUser.getProfileImageUrl());
        }

        @Test
        @DisplayName("StorageService 실패 시 예외 전파")
        void updateProfileImageStorageFailure() {
            // given — S3 업로드 실패 (빈 파일, 이미지 아닌 파일 등)
            stubActiveUser();
            MultipartFile file = mock(MultipartFile.class);
            when(storageService.uploadProfileImage(1L, file))
                    .thenThrow(new IllegalArgumentException("file is empty"));

            // when & then — StorageService의 예외가 그대로 전파됨
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> userService.updateProfileImage(1L, file));

            assertEquals("file is empty", ex.getMessage());
        }
    }

    // 4. deleteMe — 회원 탈퇴
    // soft delete 방식: User 레코드는 유지하되 status를 DELETED로 변경하고,
    // 개인정보(이메일)를 제거하며, SocialAuth 연결을 끊는다.

    @Nested
    @DisplayName("회원 탈퇴 (deleteMe)")
    class DeleteMe {

        @Test
        @DisplayName("정상 탈퇴 — 상태 DELETED, 이메일 제거, SocialAuth 삭제")
        void deleteMeSuccess() {
            // given
            stubActiveUser();

            // when
            DeleteUserResponse response = userService.deleteMe(1L);

            // then — 3가지 부수효과 모두 검증
            assertEquals(UserStatus.DELETED, response.status());
            assertEquals(UserStatus.DELETED, activeUser.getStatus());  // soft delete
            assertNull(activeUser.getEmail());                         // 개인정보 제거
            verify(socialAuthRepository).deleteByUserId(1L);           // 소셜 연결 해제
        }

        @Test
        @DisplayName("이미 DELETED 상태인 유저 탈퇴 시 예외")
        void deleteMeAlreadyDeleted() {
            // given — findActiveUser()에서 ACTIVE 검증 실패
            User deletedUser = createDeletedUser(3L);
            when(userRepository.findById(3L)).thenReturn(Optional.of(deletedUser));

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> userService.deleteMe(3L));

            assertEquals("user is not active", ex.getMessage());
        }
    }
}
