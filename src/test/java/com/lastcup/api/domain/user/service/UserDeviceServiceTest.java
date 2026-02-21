package com.lastcup.api.domain.user.service;

import com.lastcup.api.domain.user.domain.User;
import com.lastcup.api.domain.user.domain.UserDevice;
import com.lastcup.api.domain.user.domain.UserPlatform;
import com.lastcup.api.domain.user.dto.response.RegisterDeviceResponse;
import com.lastcup.api.domain.user.repository.UserDeviceRepository;
import com.lastcup.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UserDeviceService 단위 테스트.
 *
 * <p>UserDeviceService는 FCM 디바이스 토큰 등록/갱신을 담당한다.
 * createOrUpdateDevice()가 유일한 public 메서드이며, 핵심 로직:
 * - fcmToken 유효성 검증 (null/blank 불가)
 * - User 존재 확인
 * - 기존 토큰이 있으면 업데이트, 없으면 신규 생성</p>
 *
 * <p>주의: LocalDateTime.now(KST)를 내부에서 호출하므로,
 * lastSeenAt의 정확한 시각 검증은 불가 → null이 아닌지만 확인한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class UserDeviceServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserDeviceRepository userDeviceRepository;

    @InjectMocks
    private UserDeviceService userDeviceService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.create("테스트유저", "test@test.com", null);
        ReflectionTestUtils.setField(testUser, "id", 1L);
    }

    // ── 테스트 픽스처 헬퍼 ──

    private void stubUserFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    }

    // ═══════════════════════════════════════════════
    // 1. createOrUpdateDevice — 디바이스 등록/갱신
    // ═══════════════════════════════════════════════
    // 흐름: validateToken → findUser → findOrCreateByToken → updatePlatform → updateLastSeenAt → save

    @Nested
    @DisplayName("디바이스 등록/갱신 (createOrUpdateDevice)")
    class CreateOrUpdateDevice {

        @Test
        @DisplayName("신규 디바이스 등록 성공 — 기존 토큰 없음")
        void createNewDevice() {
            // given — 기존 토큰이 없으면 findByFcmToken이 empty → 새 UserDevice 생성
            stubUserFound();
            when(userDeviceRepository.findByFcmToken("new-token"))
                    .thenReturn(Optional.empty());
            when(userDeviceRepository.save(any(UserDevice.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            RegisterDeviceResponse response = userDeviceService.createOrUpdateDevice(
                    1L, "new-token", UserPlatform.ANDROID);

            // then
            assertTrue(response.success());

            // ArgumentCaptor로 save에 전달된 UserDevice 검증
            ArgumentCaptor<UserDevice> captor = ArgumentCaptor.forClass(UserDevice.class);
            verify(userDeviceRepository).save(captor.capture());
            UserDevice saved = captor.getValue();
            assertEquals("new-token", saved.getFcmToken());
            assertEquals(UserPlatform.ANDROID, saved.getPlatform());
            assertNotNull(saved.getLastSeenAt()); // now(KST) 호출됨
        }

        @Test
        @DisplayName("기존 토큰 존재 — 플랫폼과 lastSeenAt 업데이트")
        void updateExistingDevice() {
            // given — 동일 fcmToken으로 이미 등록된 디바이스가 있으면 업데이트
            stubUserFound();
            UserDevice existingDevice = UserDevice.create(testUser, "existing-token", UserPlatform.IOS);
            ReflectionTestUtils.setField(existingDevice, "id", 10L);
            when(userDeviceRepository.findByFcmToken("existing-token"))
                    .thenReturn(Optional.of(existingDevice));
            when(userDeviceRepository.save(any(UserDevice.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when — 같은 토큰으로 ANDROID 플랫폼으로 재등록
            RegisterDeviceResponse response = userDeviceService.createOrUpdateDevice(
                    1L, "existing-token", UserPlatform.ANDROID);

            // then — 기존 디바이스의 플랫폼이 ANDROID로 변경
            assertTrue(response.success());
            assertEquals(UserPlatform.ANDROID, existingDevice.getPlatform());
            assertNotNull(existingDevice.getLastSeenAt());
        }

        @Test
        @DisplayName("fcmToken이 null — 예외 발생")
        void createDeviceNullToken() {
            // given & when & then
            // validateToken()에서 null 체크 → IllegalArgumentException
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> userDeviceService.createOrUpdateDevice(1L, null, UserPlatform.ANDROID));

            assertEquals("fcmToken is blank", ex.getMessage());
            // validateToken이 가장 먼저 실행되므로 User 조회가 호출되면 안 됨
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("fcmToken이 빈 문자열 — 예외 발생")
        void createDeviceBlankToken() {
            // given & when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> userDeviceService.createOrUpdateDevice(1L, "  ", UserPlatform.IOS));

            assertEquals("fcmToken is blank", ex.getMessage());
        }

        @Test
        @DisplayName("존재하지 않는 유저 — 예외 발생")
        void createDeviceUserNotFound() {
            // given — User가 DB에 없는 경우
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> userDeviceService.createOrUpdateDevice(999L, "valid-token", UserPlatform.ANDROID));

            assertEquals("user not found", ex.getMessage());
            // User 조회 실패 후 디바이스 저장이 호출되면 안 됨
            verify(userDeviceRepository, never()).save(any());
        }
    }
}
