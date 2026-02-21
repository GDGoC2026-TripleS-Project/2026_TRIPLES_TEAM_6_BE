package com.lastcup.api.domain.auth.service;

import com.lastcup.api.domain.auth.dto.request.SocialLoginRequest;
import com.lastcup.api.domain.auth.dto.response.AuthResponse;
import com.lastcup.api.domain.auth.dto.response.AuthTokensResponse;
import com.lastcup.api.domain.user.domain.SocialAuth;
import com.lastcup.api.domain.user.domain.User;
import com.lastcup.api.domain.user.domain.UserStatus;
import com.lastcup.api.domain.user.dto.response.LoginType;
import com.lastcup.api.domain.user.repository.SocialAuthRepository;
import com.lastcup.api.domain.user.repository.UserRepository;
import com.lastcup.api.domain.user.service.UserNotificationSettingService;
import com.lastcup.api.infrastructure.oauth.OAuthTokenVerifier;
import com.lastcup.api.infrastructure.oauth.SocialProvider;
import com.lastcup.api.infrastructure.oauth.VerifiedOAuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SocialLoginService 단위 테스트.
 *
 * <p>소셜 로그인은 "신규 가입"과 "기존 유저 로그인" 두 갈래로 분기되며,
 * Apple은 최초 1회만 이메일을 제공하므로 이메일 우선순위/보충 로직이 핵심이다.</p>
 *
 * <p>@InjectMocks 대신 생성자 직접 호출하는 이유:
 * SocialLoginService는 {@code List<OAuthTokenVerifier>}를 주입받는데,
 * @InjectMocks는 List 타입의 Mock 주입을 올바르게 처리하지 못한다.
 * 따라서 @BeforeEach에서 verifier 목록을 직접 구성하여 생성자로 주입한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class SocialLoginServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private SocialAuthRepository socialAuthRepository;
    @Mock private NicknameGenerator nicknameGenerator;
    @Mock private TokenService tokenService;
    @Mock private UserNotificationSettingService notificationSettingService;
    @Mock private OAuthTokenVerifier googleVerifier;
    @Mock private OAuthTokenVerifier appleVerifier;

    private SocialLoginService socialLoginService;

    @BeforeEach
    void setUp() {
        // lenient(): 모든 테스트에서 사용되지 않더라도 불필요한 stubbing 경고를 방지
        // Mockito strict stubbing 모드에서는 사용되지 않는 stub이 있으면 예외가 발생하기 때문
        lenient().when(googleVerifier.getProvider()).thenReturn(SocialProvider.GOOGLE);
        lenient().when(appleVerifier.getProvider()).thenReturn(SocialProvider.APPLE);

        socialLoginService = new SocialLoginService(
                List.of(googleVerifier, appleVerifier),
                userRepository,
                socialAuthRepository,
                nicknameGenerator,
                tokenService,
                notificationSettingService
        );
    }

    // ── 테스트 픽스처 헬퍼 ──
    // 여러 테스트에서 반복되는 객체 생성 로직을 메서드로 추출하여 중복 제거

    private User createActiveUser(Long id, String nickname, String email) {
        User user = User.create(nickname, email, null);
        // ReflectionTestUtils: JPA 엔티티의 @Id는 DB가 자동 생성하므로
        // 테스트에서는 리플렉션으로 직접 설정해야 한다
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private User createDeletedUser(Long id, String nickname, String email) {
        User user = createActiveUser(id, nickname, email);
        ReflectionTestUtils.setField(user, "status", UserStatus.DELETED);
        return user;
    }

    private void stubTokens(Long userId) {
        when(tokenService.createTokens(userId))
                .thenReturn(new AuthTokensResponse("access-token", "refresh-token"));
    }

    // ═══════════════════════════════════════════════
    // 1. 신규 유저 회원가입 (signupNewUser)
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("신규 유저 회원가입")
    class SignupNewUser {

        @Test
        @DisplayName("Google 신규 유저 — 정상 회원가입 후 isNewUser=true 반환")
        void signupNewGoogleUser() {
            // given — OAuth 토큰 검증 결과와 DB 상태를 준비
            SocialLoginRequest request = new SocialLoginRequest("google-token", null);
            VerifiedOAuthUser verified = new VerifiedOAuthUser("google-key-123", "new@gmail.com", "https://img.url");
            when(googleVerifier.verify("google-token")).thenReturn(verified);
            when(socialAuthRepository.findByProviderAndProviderUserKey(SocialProvider.GOOGLE, "google-key-123"))
                    .thenReturn(Optional.empty()); // DB에 없음 → 신규 유저
            when(userRepository.existsByEmail("new@gmail.com")).thenReturn(false);

            User savedUser = createActiveUser(1L, "멋진 고양이", "new@gmail.com");
            when(nicknameGenerator.create()).thenReturn("멋진 고양이");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            stubTokens(1L);

            // when — 소셜 로그인 실행
            AuthResponse response = socialLoginService.login(SocialProvider.GOOGLE, request);

            // then — 신규 가입 응답 검증 + 부수효과(SocialAuth 저장, 알림설정 초기화) 검증
            assertTrue(response.isNewUser());
            assertEquals(1L, response.user().id());
            assertEquals("멋진 고양이", response.user().nickname());
            assertEquals(LoginType.SOCIAL, response.loginType());
            assertEquals(SocialProvider.GOOGLE, response.socialProvider());
            assertEquals("access-token", response.tokens().accessToken());

            // verify(): 메서드가 실제로 호출되었는지 확인 (행위 검증)
            verify(socialAuthRepository).save(any(SocialAuth.class));
            verify(notificationSettingService).ensureDefaultExists(1L);
        }

        @Test
        @DisplayName("이메일 없이 가입 허용 — Apple 이메일 미제공 시에도 회원가입 성공")
        void signupWithoutEmail() {
            // given — Apple은 재로그인 시 이메일을 제공하지 않을 수 있다
            SocialLoginRequest request = new SocialLoginRequest("apple-token", null);
            VerifiedOAuthUser verified = new VerifiedOAuthUser("apple-key-456", null, null);
            when(appleVerifier.verify("apple-token")).thenReturn(verified);
            when(socialAuthRepository.findByProviderAndProviderUserKey(SocialProvider.APPLE, "apple-key-456"))
                    .thenReturn(Optional.empty());

            User savedUser = createActiveUser(2L, "빠른 토끼", null);
            when(nicknameGenerator.create()).thenReturn("빠른 토끼");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            stubTokens(2L);

            // when
            AuthResponse response = socialLoginService.login(SocialProvider.APPLE, request);

            // then — 이메일 null이면 중복 체크 자체를 스킵해야 한다
            assertTrue(response.isNewUser());
            verify(userRepository, never()).existsByEmail(any());
        }

        @Test
        @DisplayName("이메일 중복 시 예외 발생")
        void signupDuplicateEmailThrows() {
            // given — 이미 같은 이메일로 가입한 유저가 존재
            SocialLoginRequest request = new SocialLoginRequest("google-token", null);
            VerifiedOAuthUser verified = new VerifiedOAuthUser("google-key-789", "dup@gmail.com", null);
            when(googleVerifier.verify("google-token")).thenReturn(verified);
            when(socialAuthRepository.findByProviderAndProviderUserKey(SocialProvider.GOOGLE, "google-key-789"))
                    .thenReturn(Optional.empty());
            when(userRepository.existsByEmail("dup@gmail.com")).thenReturn(true);

            // when & then — 중복 이메일은 회원가입 차단
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> socialLoginService.login(SocialProvider.GOOGLE, request));

            assertEquals("email already exists", ex.getMessage());
            verify(userRepository, never()).save(any()); // 저장이 일어나면 안 됨
        }

        @Test
        @DisplayName("빈 문자열 이메일은 null과 동일하게 중복 체크 스킵")
        void signupBlankEmailSkipsDuplicateCheck() {
            // given — request.email()이 blank("  ")이고, ID Token에서도 이메일 없음
            // SocialLoginService.login()에서 이메일 우선순위: verified.email() > request.email()
            // verified.email()이 null이므로 resolved.email()은 "  " (blank)
            // signupNewUser()에서 isBlank() 체크로 중복 검사를 스킵한다
            SocialLoginRequest request = new SocialLoginRequest("apple-token", "  ");
            VerifiedOAuthUser verified = new VerifiedOAuthUser("apple-key-blank", null, null);
            when(appleVerifier.verify("apple-token")).thenReturn(verified);
            when(socialAuthRepository.findByProviderAndProviderUserKey(SocialProvider.APPLE, "apple-key-blank"))
                    .thenReturn(Optional.empty());

            User savedUser = createActiveUser(3L, "조용한 판다", null);
            when(nicknameGenerator.create()).thenReturn("조용한 판다");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            stubTokens(3L);

            // when
            AuthResponse response = socialLoginService.login(SocialProvider.APPLE, request);

            // then
            assertTrue(response.isNewUser());
            verify(userRepository, never()).existsByEmail(any());
        }
    }

    // ═══════════════════════════════════════════════
    // 2. 기존 유저 로그인 (loginExistingUser)
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("기존 유저 로그인")
    class LoginExistingUser {

        @Test
        @DisplayName("기존 Google 유저 — 정상 로그인, isNewUser=false")
        void loginExistingGoogleUser() {
            // given — DB에 이미 SocialAuth가 존재하는 유저
            SocialLoginRequest request = new SocialLoginRequest("google-token", null);
            VerifiedOAuthUser verified = new VerifiedOAuthUser("google-key-123", "exist@gmail.com", null);
            when(googleVerifier.verify("google-token")).thenReturn(verified);

            SocialAuth socialAuth = SocialAuth.create(10L, SocialProvider.GOOGLE, "google-key-123", "exist@gmail.com");
            when(socialAuthRepository.findByProviderAndProviderUserKey(SocialProvider.GOOGLE, "google-key-123"))
                    .thenReturn(Optional.of(socialAuth)); // DB에 있음 → 기존 유저

            User existingUser = createActiveUser(10L, "기존유저", "exist@gmail.com");
            when(userRepository.findById(10L)).thenReturn(Optional.of(existingUser));
            stubTokens(10L);

            // when
            AuthResponse response = socialLoginService.login(SocialProvider.GOOGLE, request);

            // then — 기존 유저이므로 isNewUser=false
            assertFalse(response.isNewUser());
            assertEquals(10L, response.user().id());
            assertEquals("기존유저", response.user().nickname());
            assertEquals(SocialProvider.GOOGLE, response.socialProvider());
        }

        @Test
        @DisplayName("DELETED 상태 유저 로그인 시 예외 발생")
        void loginDeletedUserThrows() {
            // given — SocialAuth는 남아있지만 User가 탈퇴(DELETED) 상태
            SocialLoginRequest request = new SocialLoginRequest("google-token", null);
            VerifiedOAuthUser verified = new VerifiedOAuthUser("google-key-del", "del@gmail.com", null);
            when(googleVerifier.verify("google-token")).thenReturn(verified);

            SocialAuth socialAuth = SocialAuth.create(20L, SocialProvider.GOOGLE, "google-key-del", "del@gmail.com");
            when(socialAuthRepository.findByProviderAndProviderUserKey(SocialProvider.GOOGLE, "google-key-del"))
                    .thenReturn(Optional.of(socialAuth));

            User deletedUser = createDeletedUser(20L, "탈퇴유저", "del@gmail.com");
            when(userRepository.findById(20L)).thenReturn(Optional.of(deletedUser));

            // when & then — 탈퇴 유저는 로그인 차단
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> socialLoginService.login(SocialProvider.GOOGLE, request));

            assertEquals("user is not active", ex.getMessage());
        }

        @Test
        @DisplayName("SocialAuth는 있으나 User가 삭제된 경우 예외 발생")
        void loginSocialAuthExistsButUserNotFoundThrows() {
            // given — SocialAuth 레코드는 있지만 User 테이블에서 삭제된 비정상 데이터
            // 실무에서 CASCADE 미설정 시 발생할 수 있는 데이터 정합성 문제
            SocialLoginRequest request = new SocialLoginRequest("google-token", null);
            VerifiedOAuthUser verified = new VerifiedOAuthUser("google-key-orphan", "orphan@gmail.com", null);
            when(googleVerifier.verify("google-token")).thenReturn(verified);

            SocialAuth socialAuth = SocialAuth.create(99L, SocialProvider.GOOGLE, "google-key-orphan", "orphan@gmail.com");
            when(socialAuthRepository.findByProviderAndProviderUserKey(SocialProvider.GOOGLE, "google-key-orphan"))
                    .thenReturn(Optional.of(socialAuth));
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            // when & then — IllegalStateException: 복구 불가능한 시스템 오류
            // IllegalArgumentException(클라이언트 잘못)과 구분하여 IllegalStateException 사용
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> socialLoginService.login(SocialProvider.GOOGLE, request));

            assertEquals("user not found", ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════
    // 3. Apple 이메일 우선순위 및 보충 로직
    // ═══════════════════════════════════════════════
    // Apple은 최초 인가 1회에만 이메일을 제공하고, 이후에는 절대 재전달하지 않는다.
    // 이메일 결정 우선순위: (1) ID Token claim > (2) 클라이언트 request body

    @Nested
    @DisplayName("Apple 이메일 처리")
    class AppleEmailHandling {

        @Test
        @DisplayName("ID Token 이메일이 request 이메일보다 우선")
        void idTokenEmailTakesPriority() {
            // given — ID Token에 이메일이 있고, request body에도 이메일이 있는 경우
            // verified.email()이 null이 아니므로 token@apple.com이 선택되어야 한다
            SocialLoginRequest request = new SocialLoginRequest("apple-token", "client@apple.com");
            VerifiedOAuthUser verified = new VerifiedOAuthUser("apple-key-prio", "token@apple.com", null);
            when(appleVerifier.verify("apple-token")).thenReturn(verified);
            when(socialAuthRepository.findByProviderAndProviderUserKey(SocialProvider.APPLE, "apple-key-prio"))
                    .thenReturn(Optional.empty());
            when(userRepository.existsByEmail("token@apple.com")).thenReturn(false);

            User savedUser = createActiveUser(5L, "우선순위", "token@apple.com");
            when(nicknameGenerator.create()).thenReturn("우선순위");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            stubTokens(5L);

            // when
            socialLoginService.login(SocialProvider.APPLE, request);

            // then — token@apple.com으로 중복 체크 (client@apple.com이 아님)
            verify(userRepository).existsByEmail("token@apple.com");
        }

        @Test
        @DisplayName("ID Token 이메일이 null이면 request 이메일을 fallback으로 사용")
        void requestEmailUsedAsFallback() {
            // given — ID Token에 이메일 없음(null), request body에만 이메일 존재
            // Apple 최초 인가 시 iOS 클라이언트가 ASAuthorizationAppleIDCredential.email을
            // request body에 담아서 보내는 시나리오
            SocialLoginRequest request = new SocialLoginRequest("apple-token", "fallback@apple.com");
            VerifiedOAuthUser verified = new VerifiedOAuthUser("apple-key-fb", null, null);
            when(appleVerifier.verify("apple-token")).thenReturn(verified);
            when(socialAuthRepository.findByProviderAndProviderUserKey(SocialProvider.APPLE, "apple-key-fb"))
                    .thenReturn(Optional.empty());
            when(userRepository.existsByEmail("fallback@apple.com")).thenReturn(false);

            User savedUser = createActiveUser(6L, "폴백유저", "fallback@apple.com");
            when(nicknameGenerator.create()).thenReturn("폴백유저");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            stubTokens(6L);

            // when
            socialLoginService.login(SocialProvider.APPLE, request);

            // then — fallback 이메일로 중복 체크 수행
            verify(userRepository).existsByEmail("fallback@apple.com");
        }

        @Test
        @DisplayName("기존 유저에 이메일이 없을 때 — 이메일 보충 업데이트")
        void supplementEmailForExistingUserWithoutEmail() {
            // given — 첫 가입 시 이메일 없이 생성된 유저가 이후 로그인할 때
            // 이번에는 ID Token에 이메일이 포함된 경우 → User, SocialAuth 양쪽에 보충
            SocialLoginRequest request = new SocialLoginRequest("apple-token", null);
            VerifiedOAuthUser verified = new VerifiedOAuthUser("apple-key-sup", "supplement@apple.com", null);
            when(appleVerifier.verify("apple-token")).thenReturn(verified);

            SocialAuth socialAuth = SocialAuth.create(30L, SocialProvider.APPLE, "apple-key-sup", null);
            when(socialAuthRepository.findByProviderAndProviderUserKey(SocialProvider.APPLE, "apple-key-sup"))
                    .thenReturn(Optional.of(socialAuth));

            User userWithoutEmail = createActiveUser(30L, "이메일없음", null);
            when(userRepository.findById(30L)).thenReturn(Optional.of(userWithoutEmail));
            stubTokens(30L);

            // when
            socialLoginService.login(SocialProvider.APPLE, request);

            // then — User.updateEmailIfAbsent()와 SocialAuth.updateEmailIfAbsent()가 호출되어
            // 이메일이 보충되었는지 실제 객체 상태로 검증 (상태 검증 > 행위 검증)
            assertEquals("supplement@apple.com", userWithoutEmail.getEmail());
            assertEquals("supplement@apple.com", socialAuth.getEmail());
        }

        @Test
        @DisplayName("기존 유저에 이메일이 이미 있으면 — 보충하지 않음")
        void noSupplementWhenEmailAlreadyExists() {
            // given — 이미 이메일이 있는 유저에게 다른 이메일이 들어와도 덮어쓰지 않는다
            // updateEmailIfAbsent()는 기존 이메일이 있으면 false를 반환하고 무시
            SocialLoginRequest request = new SocialLoginRequest("apple-token", null);
            VerifiedOAuthUser verified = new VerifiedOAuthUser("apple-key-exist", "new@apple.com", null);
            when(appleVerifier.verify("apple-token")).thenReturn(verified);

            SocialAuth socialAuth = SocialAuth.create(31L, SocialProvider.APPLE, "apple-key-exist", "old@apple.com");
            when(socialAuthRepository.findByProviderAndProviderUserKey(SocialProvider.APPLE, "apple-key-exist"))
                    .thenReturn(Optional.of(socialAuth));

            User userWithEmail = createActiveUser(31L, "이메일있음", "old@apple.com");
            when(userRepository.findById(31L)).thenReturn(Optional.of(userWithEmail));
            stubTokens(31L);

            // when
            socialLoginService.login(SocialProvider.APPLE, request);

            // then — 기존 이메일이 유지되어야 한다
            assertEquals("old@apple.com", userWithEmail.getEmail());
            assertEquals("old@apple.com", socialAuth.getEmail());
        }
    }

    // ═══════════════════════════════════════════════
    // 4. Provider Verifier 조회
    // ═══════════════════════════════════════════════

    @Nested
    @DisplayName("Provider Verifier 조회")
    class FindVerifier {

        @Test
        @DisplayName("지원하지 않는 provider 요청 시 예외 발생")
        void unsupportedProviderThrows() {
            // given — setUp()에서 GOOGLE, APPLE만 등록했으므로 KAKAO는 미지원
            SocialLoginRequest request = new SocialLoginRequest("kakao-token", null);

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> socialLoginService.login(SocialProvider.KAKAO, request));

            assertEquals("unsupported provider", ex.getMessage());
        }
    }
}
