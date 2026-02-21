package com.lastcup.api.domain.notification.service;

import com.lastcup.api.domain.notification.domain.NotificationDispatchLog;
import com.lastcup.api.domain.notification.domain.NotificationType;
import com.lastcup.api.domain.notification.repository.NotificationDispatchLogRepository;
import com.lastcup.api.domain.user.domain.UserNotificationSetting;
import com.lastcup.api.domain.user.repository.UserDeviceRepository;
import com.lastcup.api.domain.user.repository.UserDeviceRepository.UserDeviceTokenProjection;
import com.lastcup.api.domain.user.repository.UserNotificationSettingRepository;
import com.lastcup.api.infrastructure.notification.FcmNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UserNotificationScheduler 단위 테스트.
 *
 * <p>스케줄러는 매분 실행되며 두 종류의 알림을 처리한다:</p>
 * <ul>
 *   <li>RECORD_REMIND — "기록 알림": 사용자가 설정한 시각에 기록 독려</li>
 *   <li>DAILY_CLOSE — "마감 알림": 하루 섭취 기록 마감 독려</li>
 * </ul>
 *
 * <p>핵심 설계 원칙:
 * <ol>
 *   <li>중복 발송 방지 — dispatch log로 오늘 이미 보낸 유저 스킵</li>
 *   <li>장애 격리 — 한 유저의 FCM 실패가 다른 유저에 영향 주지 않음</li>
 *   <li>토큰 정합성 — null/blank 토큰 필터링, 중복 토큰 제거</li>
 *   <li>멱등성 — dispatch log 중복 저장 시 DataIntegrityViolationException 무시</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class UserNotificationSchedulerTest {

    @Mock private UserNotificationSettingRepository settingRepository;
    @Mock private UserDeviceRepository deviceRepository;
    @Mock private NotificationDispatchLogRepository dispatchLogRepository;
    @Mock private FcmNotificationService fcmNotificationService;

    @InjectMocks
    private UserNotificationScheduler scheduler;

    // ── 테스트 픽스처 헬퍼 ──

    private UserNotificationSetting createSetting(Long userId) {
        return UserNotificationSetting.createDefault(
                userId, true, LocalTime.of(9, 0), LocalTime.of(21, 0)
        );
    }

    /**
     * Spring Data JPA Projection 인터페이스의 테스트용 구현.
     * 실제 DB 쿼리 결과 대신 익명 클래스로 원하는 값을 반환한다.
     */
    private UserDeviceTokenProjection createTokenProjection(Long userId, String fcmToken) {
        return new UserDeviceTokenProjection() {
            @Override
            public Long getUserId() { return userId; }
            @Override
            public String getFcmToken() { return fcmToken; }
        };
    }

    /**
     * 대부분의 테스트에서 DAILY_CLOSE 경로는 빈 리스트를 반환하도록 설정.
     * RECORD_REMIND 경로만 테스트하기 위한 헬퍼.
     */
    private void stubDailyCloseEmpty() {
        when(settingRepository.findAllByIsEnabledTrueAndDailyCloseAt(any()))
                .thenReturn(Collections.emptyList());
    }

    // ═══════════════════════════════════════════════
    // 1. sendScheduledNotifications — 스케줄러 진입점
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("스케줄러 진입점 (sendScheduledNotifications)")
    class ScheduledEntry {

        @Test
        @DisplayName("스케줄러 실행 시 기록 알림과 마감 알림 양쪽 모두 조회")
        void callsBothRemindAndClose() {
            // given — 양쪽 모두 대상 유저 없음
            when(settingRepository.findAllByIsEnabledTrueAndRecordRemindAt(any(LocalTime.class)))
                    .thenReturn(Collections.emptyList());
            when(settingRepository.findAllByIsEnabledTrueAndDailyCloseAt(any(LocalTime.class)))
                    .thenReturn(Collections.emptyList());

            // when
            scheduler.sendScheduledNotifications();

            // then — sendRecordRemind, sendDailyClose 양쪽 모두 호출 확인
            verify(settingRepository).findAllByIsEnabledTrueAndRecordRemindAt(any(LocalTime.class));
            verify(settingRepository).findAllByIsEnabledTrueAndDailyCloseAt(any(LocalTime.class));
        }
    }

    // ═══════════════════════════════════════════════
    // 2. processSettings — 핵심 알림 발송 로직
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("알림 발송 로직 (processSettings)")
    class ProcessSettings {

        @Test
        @DisplayName("대상 유저가 없으면 아무것도 하지 않음")
        void emptySettingsDoesNothing() {
            // given — settings.isEmpty() == true이므로 early return
            when(settingRepository.findAllByIsEnabledTrueAndRecordRemindAt(any(LocalTime.class)))
                    .thenReturn(Collections.emptyList());
            when(settingRepository.findAllByIsEnabledTrueAndDailyCloseAt(any(LocalTime.class)))
                    .thenReturn(Collections.emptyList());

            // when
            scheduler.sendScheduledNotifications();

            // then — 하위 의존성에 아무 호출도 없어야 한다
            // verifyNoInteractions(): Mock 객체에 어떤 메서드도 호출되지 않았음을 검증
            verifyNoInteractions(deviceRepository);
            verifyNoInteractions(fcmNotificationService);
            verifyNoInteractions(dispatchLogRepository);
        }

        @Test
        @DisplayName("정상 발송 — FCM 호출 후 dispatch log 저장")
        void successfulSend() {
            // given
            UserNotificationSetting setting = createSetting(1L);
            when(settingRepository.findAllByIsEnabledTrueAndRecordRemindAt(any()))
                    .thenReturn(List.of(setting));
            stubDailyCloseEmpty();

            when(dispatchLogRepository.findSentUserIdsByTypeAndDateInUsers(
                    eq(NotificationType.RECORD_REMIND), any(), any()))
                    .thenReturn(Collections.emptyList()); // 오늘 아직 발송 안 됨

            when(deviceRepository.findEnabledTokensByUserIds(any()))
                    .thenReturn(List.of(createTokenProjection(1L, "token-abc")));

            // when
            scheduler.sendScheduledNotifications();

            // then — FCM 발송 + dispatch log 저장
            verify(fcmNotificationService).sendToTokens(
                    eq(List.of("token-abc")), eq("기록 알림"), eq("오늘의 기록을 남겨보세요."));
            verify(dispatchLogRepository).save(any(NotificationDispatchLog.class));
        }

        @Test
        @DisplayName("오늘 이미 발송된 유저는 스킵")
        void skipAlreadySentUser() {
            // given — dispatchLog에 userId=1이 이미 존재
            UserNotificationSetting setting = createSetting(1L);
            when(settingRepository.findAllByIsEnabledTrueAndRecordRemindAt(any()))
                    .thenReturn(List.of(setting));
            stubDailyCloseEmpty();

            when(dispatchLogRepository.findSentUserIdsByTypeAndDateInUsers(
                    eq(NotificationType.RECORD_REMIND), any(), any()))
                    .thenReturn(List.of(1L)); // 이미 발송됨

            when(deviceRepository.findEnabledTokensByUserIds(any()))
                    .thenReturn(List.of(createTokenProjection(1L, "token-abc")));

            // when
            scheduler.sendScheduledNotifications();

            // then — sentUserIds에 포함되어 있으므로 FCM 호출 안 됨
            verifyNoInteractions(fcmNotificationService);
        }

        @Test
        @DisplayName("디바이스 토큰이 없는 유저는 스킵")
        void skipUserWithoutTokens() {
            // given — 유저는 알림 설정이 있지만 등록된 디바이스가 없음
            UserNotificationSetting setting = createSetting(1L);
            when(settingRepository.findAllByIsEnabledTrueAndRecordRemindAt(any()))
                    .thenReturn(List.of(setting));
            stubDailyCloseEmpty();

            when(dispatchLogRepository.findSentUserIdsByTypeAndDateInUsers(any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(deviceRepository.findEnabledTokensByUserIds(any()))
                    .thenReturn(Collections.emptyList()); // 토큰 없음

            // when
            scheduler.sendScheduledNotifications();

            // then
            verifyNoInteractions(fcmNotificationService);
        }

        @Test
        @DisplayName("FCM 발송 실패 시 해당 유저만 실패, 다른 유저는 계속 발송")
        void fcmFailureIsolated() {
            // given — 2명의 유저, user 1은 FCM 실패, user 2는 성공
            // processSettings()에서 try-catch로 유저별 장애 격리
            UserNotificationSetting setting1 = createSetting(1L);
            UserNotificationSetting setting2 = createSetting(2L);
            when(settingRepository.findAllByIsEnabledTrueAndRecordRemindAt(any()))
                    .thenReturn(List.of(setting1, setting2));
            stubDailyCloseEmpty();

            when(dispatchLogRepository.findSentUserIdsByTypeAndDateInUsers(any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(deviceRepository.findEnabledTokensByUserIds(any()))
                    .thenReturn(List.of(
                            createTokenProjection(1L, "token-1"),
                            createTokenProjection(2L, "token-2")
                    ));

            doThrow(new RuntimeException("FCM error"))
                    .when(fcmNotificationService).sendToTokens(eq(List.of("token-1")), any(), any());
            doNothing()
                    .when(fcmNotificationService).sendToTokens(eq(List.of("token-2")), any(), any());

            // when — 예외가 전파되지 않아야 함
            assertDoesNotThrow(() -> scheduler.sendScheduledNotifications());

            // then — user 1은 실패했지만 user 2의 dispatch log는 저장되어야 함
            verify(dispatchLogRepository).save(any(NotificationDispatchLog.class));
        }

        @Test
        @DisplayName("dispatch log 중복 저장 시 DataIntegrityViolationException 무시")
        void duplicateDispatchLogIgnored() {
            // given — saveDispatchLog()에서 unique constraint 위반 시
            // DataIntegrityViolationException을 catch하고 log.info()만 남김 (멱등성 보장)
            UserNotificationSetting setting = createSetting(1L);
            when(settingRepository.findAllByIsEnabledTrueAndRecordRemindAt(any()))
                    .thenReturn(List.of(setting));
            stubDailyCloseEmpty();

            when(dispatchLogRepository.findSentUserIdsByTypeAndDateInUsers(any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(deviceRepository.findEnabledTokensByUserIds(any()))
                    .thenReturn(List.of(createTokenProjection(1L, "token-abc")));

            when(dispatchLogRepository.save(any(NotificationDispatchLog.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate"));

            // when — 예외가 전파되지 않아야 함
            assertDoesNotThrow(() -> scheduler.sendScheduledNotifications());

            // then — FCM 발송은 정상 수행됨 (dispatch log 실패와 무관)
            verify(fcmNotificationService).sendToTokens(any(), any(), any());
        }
    }

    // ═══════════════════════════════════════════════
    // 3. groupEnabledTokensByUserId — 토큰 그룹핑
    // ═══════════════════════════════════════════════
    // HashMap<userId, Set<token>> → HashMap<userId, List<token>>으로 변환하며,
    // Set을 사용하므로 자연스럽게 중복이 제거된다.

    @Nested
    @DisplayName("토큰 그룹핑 (groupEnabledTokensByUserId)")
    class TokenGrouping {

        @Test
        @DisplayName("null/blank 토큰은 필터링됨")
        void filtersNullAndBlankTokens() {
            // given — 유효한 토큰이 하나도 없는 경우
            UserNotificationSetting setting = createSetting(1L);
            when(settingRepository.findAllByIsEnabledTrueAndRecordRemindAt(any()))
                    .thenReturn(List.of(setting));
            stubDailyCloseEmpty();

            when(dispatchLogRepository.findSentUserIdsByTypeAndDateInUsers(any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(deviceRepository.findEnabledTokensByUserIds(any()))
                    .thenReturn(List.of(
                            createTokenProjection(1L, null),  // null 토큰
                            createTokenProjection(1L, "  "),  // blank 토큰
                            createTokenProjection(1L, "")     // empty 토큰
                    ));

            // when
            scheduler.sendScheduledNotifications();

            // then — 유효한 토큰이 없으므로 FCM 호출 안 됨
            verifyNoInteractions(fcmNotificationService);
        }

        @Test
        @DisplayName("동일 유저의 중복 토큰은 제거됨")
        void deduplicatesTokensPerUser() {
            // given — 같은 디바이스 토큰이 2번 조회됨 (데이터 정합성 문제)
            // groupEnabledTokensByUserId()에서 Set으로 수집하므로 자동 중복 제거
            UserNotificationSetting setting = createSetting(1L);
            when(settingRepository.findAllByIsEnabledTrueAndRecordRemindAt(any()))
                    .thenReturn(List.of(setting));
            stubDailyCloseEmpty();

            when(dispatchLogRepository.findSentUserIdsByTypeAndDateInUsers(any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(deviceRepository.findEnabledTokensByUserIds(any()))
                    .thenReturn(List.of(
                            createTokenProjection(1L, "same-token"),
                            createTokenProjection(1L, "same-token")
                    ));

            // when
            scheduler.sendScheduledNotifications();

            // then — ArgumentCaptor: verify() 시점에 실제 전달된 인자를 캡처하여 검증
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String>> tokenCaptor = ArgumentCaptor.forClass(List.class);
            verify(fcmNotificationService).sendToTokens(tokenCaptor.capture(), any(), any());

            assertEquals(1, tokenCaptor.getValue().size()); // 중복 제거되어 1개
            assertEquals("same-token", tokenCaptor.getValue().get(0));
        }

        @Test
        @DisplayName("여러 유저의 토큰이 유저별로 올바르게 그룹핑")
        void groupsByUserId() {
            // given — 2명의 유저가 각각 다른 토큰을 가짐
            UserNotificationSetting setting1 = createSetting(1L);
            UserNotificationSetting setting2 = createSetting(2L);
            when(settingRepository.findAllByIsEnabledTrueAndRecordRemindAt(any()))
                    .thenReturn(List.of(setting1, setting2));
            stubDailyCloseEmpty();

            when(dispatchLogRepository.findSentUserIdsByTypeAndDateInUsers(any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(deviceRepository.findEnabledTokensByUserIds(any()))
                    .thenReturn(List.of(
                            createTokenProjection(1L, "token-user1"),
                            createTokenProjection(2L, "token-user2")
                    ));

            // when
            scheduler.sendScheduledNotifications();

            // then — 유저별로 별도 FCM 호출 + 각각 dispatch log 저장
            verify(fcmNotificationService).sendToTokens(eq(List.of("token-user1")), any(), any());
            verify(fcmNotificationService).sendToTokens(eq(List.of("token-user2")), any(), any());
            verify(dispatchLogRepository, times(2)).save(any(NotificationDispatchLog.class));
        }
    }

    // ═══════════════════════════════════════════════
    // 4. 마감 알림 (DAILY_CLOSE) 경로 검증
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("마감 알림 (dailyClose)")
    class DailyClose {

        @Test
        @DisplayName("마감 알림 정상 발송 — 제목/본문 검증")
        void dailyCloseSuccessfulSend() {
            // given — RECORD_REMIND는 없고 DAILY_CLOSE만 대상
            UserNotificationSetting setting = createSetting(1L);
            when(settingRepository.findAllByIsEnabledTrueAndRecordRemindAt(any()))
                    .thenReturn(Collections.emptyList());
            when(settingRepository.findAllByIsEnabledTrueAndDailyCloseAt(any()))
                    .thenReturn(List.of(setting));

            when(dispatchLogRepository.findSentUserIdsByTypeAndDateInUsers(
                    eq(NotificationType.DAILY_CLOSE), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(deviceRepository.findEnabledTokensByUserIds(any()))
                    .thenReturn(List.of(createTokenProjection(1L, "token-close")));

            // when
            scheduler.sendScheduledNotifications();

            // then — DAILY_CLOSE 전용 제목/본문이 사용되었는지 검증
            verify(fcmNotificationService).sendToTokens(
                    eq(List.of("token-close")), eq("마감 알림"), eq("오늘의 섭취를 마감해 주세요."));
        }
    }
}
