package com.lastcup.api.domain.user.service;

import com.lastcup.api.domain.user.domain.UserNotificationSetting;
import com.lastcup.api.domain.user.dto.response.NotificationSettingResponse;
import com.lastcup.api.domain.user.dto.response.UpdateNotificationSettingResponse;
import com.lastcup.api.domain.user.repository.UserNotificationSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserNotificationSettingService 단위 테스트.
 * 알림 설정 조회/수정, 기본값 생성, 부분 업데이트 로직을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class UserNotificationSettingServiceTest {

    @Mock private UserNotificationSettingRepository repository;

    @InjectMocks
    private UserNotificationSettingService service;

    // ── 상수: 서비스 내부 기본값과 동일 ──
    private static final LocalTime DEFAULT_RECORD_REMIND_AT = LocalTime.of(14, 0, 0);
    private static final LocalTime DEFAULT_DAILY_CLOSE_AT = LocalTime.of(21, 0, 0);

    // ── 테스트 픽스처 헬퍼 ──

    /**
     * UserNotificationSetting은 @Id가 userId(Long)이므로
     * ReflectionTestUtils 없이 createDefault()로 직접 생성 가능.
     */
    private UserNotificationSetting createSetting(Long userId, boolean enabled,
                                                   LocalTime remindAt, LocalTime closeAt) {
        return UserNotificationSetting.createDefault(userId, enabled, remindAt, closeAt);
    }

    private UserNotificationSetting createDefaultSetting(Long userId) {
        return createSetting(userId, true, DEFAULT_RECORD_REMIND_AT, DEFAULT_DAILY_CLOSE_AT);
    }

    // 1. findOrCreate — 알림 설정 조회 (없으면 생성)
    // repository.findById() → 있으면 반환 / 없으면 createDefault() → save → 반환

    @Nested
    @DisplayName("알림 설정 조회 (findOrCreate)")
    class FindOrCreate {

        @Test
        @DisplayName("기존 설정 존재 — 그대로 반환")
        void findOrCreateExisting() {
            // given — DB에 이미 설정이 있는 경우
            UserNotificationSetting existing = createSetting(
                    1L, false, LocalTime.of(9, 0), LocalTime.of(22, 0));
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            // when
            NotificationSettingResponse response = service.findOrCreate(1L);

            // then — DB 조회값이 그대로 반환
            assertFalse(response.isEnabled());
            assertEquals(LocalTime.of(9, 0), response.recordRemindAt());
            assertEquals(LocalTime.of(22, 0), response.dailyCloseAt());
            // 기존 설정이 있으므로 save가 호출되면 안 됨
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("기존 설정 없음 — 기본값 생성 후 반환")
        void findOrCreateNew() {
            // given — findById가 empty → createDefault() 호출
            when(repository.findById(1L)).thenReturn(Optional.empty());
            // save()는 전달받은 엔티티를 그대로 반환하도록 스텁
            when(repository.save(any(UserNotificationSetting.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            NotificationSettingResponse response = service.findOrCreate(1L);

            // then — 기획 회의 정의 기본값
            assertTrue(response.isEnabled());
            assertEquals(DEFAULT_RECORD_REMIND_AT, response.recordRemindAt());
            assertEquals(DEFAULT_DAILY_CLOSE_AT, response.dailyCloseAt());
            verify(repository).save(any(UserNotificationSetting.class));
        }
    }

    // 2. update — 알림 설정 수정
    // update()는 null 파라미터를 무시하는 "부분 업데이트" 패턴을 사용한다.
    // 즉, isEnabled만 보내면 시간 설정은 기존값 유지.

    @Nested
    @DisplayName("알림 설정 수정 (update)")
    class Update {

        @Test
        @DisplayName("기존 설정 있음 — 전체 필드 수정")
        void updateExisting() {
            // given
            UserNotificationSetting existing = createDefaultSetting(1L);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(repository.save(any(UserNotificationSetting.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when — 모든 필드를 새 값으로 변경
            UpdateNotificationSettingResponse response = service.update(
                    1L, false, LocalTime.of(10, 0), LocalTime.of(23, 0));

            // then
            assertTrue(response.updated());
            // 엔티티 상태 직접 검증 (상태 검증)
            assertFalse(existing.isEnabled());
            assertEquals(LocalTime.of(10, 0), existing.getRecordRemindAt());
            assertEquals(LocalTime.of(23, 0), existing.getDailyCloseAt());
        }

        @Test
        @DisplayName("기존 설정 없음 — 기본값 생성 후 수정")
        void updateCreateThenModify() {
            // given — 설정이 없으면 createDefault()로 생성 후 update() 적용
            when(repository.findById(1L)).thenReturn(Optional.empty());
            when(repository.save(any(UserNotificationSetting.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            UpdateNotificationSettingResponse response = service.update(
                    1L, false, LocalTime.of(8, 0), LocalTime.of(20, 0));

            // then — save가 2번 호출됨 (createDefault에서 1번 + update에서 1번)
            assertTrue(response.updated());
            verify(repository, times(2)).save(any(UserNotificationSetting.class));
        }

        @Test
        @DisplayName("부분 업데이트 — null 필드는 기존값 유지")
        void updatePartial() {
            // given — 기존 설정: enabled=true, 14:00, 21:00
            UserNotificationSetting existing = createDefaultSetting(1L);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(repository.save(any(UserNotificationSetting.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when — isEnabled만 false로 변경, 나머지는 null (기존값 유지)
            service.update(1L, false, null, null);

            // then — isEnabled만 변경, 시간은 기존 기본값 유지
            assertFalse(existing.isEnabled());
            assertEquals(DEFAULT_RECORD_REMIND_AT, existing.getRecordRemindAt());
            assertEquals(DEFAULT_DAILY_CLOSE_AT, existing.getDailyCloseAt());
        }
    }

    // 3. ensureDefaultExists — 기본 설정 보장 (회원가입 시 호출)
    // 멱등성 보장: 이미 존재하면 아무것도 안 함 (existsById로 체크)

    @Nested
    @DisplayName("기본 설정 보장 (ensureDefaultExists)")
    class EnsureDefaultExists {

        @Test
        @DisplayName("이미 존재 — 아무 동작 안 함 (멱등성)")
        void ensureDefaultExistsAlreadyExists() {
            // given — existsById가 true → early return
            when(repository.existsById(1L)).thenReturn(true);

            // when
            service.ensureDefaultExists(1L);

            // then — save가 호출되지 않아야 한다
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않음 — 기본값 생성")
        void ensureDefaultExistsCreatesNew() {
            // given
            when(repository.existsById(1L)).thenReturn(false);
            when(repository.save(any(UserNotificationSetting.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            service.ensureDefaultExists(1L);

            // then — 기본값으로 save 호출됨
            verify(repository).save(any(UserNotificationSetting.class));
        }
    }
}
