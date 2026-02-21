package com.lastcup.api.domain.auth.service;

import com.lastcup.api.domain.auth.config.PasswordResetProperties;
import com.lastcup.api.domain.auth.domain.PasswordResetToken;
import com.lastcup.api.domain.auth.dto.request.PasswordResetConfirmRequest;
import com.lastcup.api.domain.auth.dto.request.PasswordResetRequest;
import com.lastcup.api.domain.auth.dto.request.PasswordResetVerifyRequest;
import com.lastcup.api.domain.auth.repository.PasswordResetTokenRepository;
import com.lastcup.api.domain.user.domain.LocalAuth;
import com.lastcup.api.domain.user.domain.User;
import com.lastcup.api.domain.user.repository.LocalAuthRepository;
import com.lastcup.api.domain.user.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * PasswordResetService 단위 테스트.
 * 요청 → 검증 → 확정 3단계 비밀번호 재설정 로직을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private LocalAuthRepository localAuthRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JavaMailSender mailSender;
    @Mock private PasswordResetProperties properties;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;
    private LocalAuth testLocalAuth;

    @BeforeEach
    void setUp() {
        testUser = User.create("테스트유저", "user@test.com", null);
        ReflectionTestUtils.setField(testUser, "id", 1L);

        // LocalAuth의 PK는 userId (별도 auto-increment 아님)
        testLocalAuth = LocalAuth.create(1L, "testlogin", "hashed-pw");
    }

    // ── 테스트 픽스처 헬퍼 ──

    /**
     * 대부분의 테스트에서 공통으로 필요한 User/LocalAuth 조회 stubbing.
     * getValidToken() 내부에서 매번 findByEmail + findByLoginId를 호출하기 때문.
     */
    private void stubUserAndLocalAuth() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(localAuthRepository.findByLoginId("testlogin")).thenReturn(Optional.of(testLocalAuth));
    }

    /**
     * 테스트용 PasswordResetToken 생성.
     * expired=true면 이미 만료된 토큰, used=true면 이미 사용된 토큰을 생성한다.
     */
    private PasswordResetToken createToken(Long userId, String code, boolean expired, boolean used) {
        LocalDateTime expiresAt = expired
                ? LocalDateTime.now().minusMinutes(1)  // 1분 전에 만료
                : LocalDateTime.now().plusMinutes(30);  // 30분 후 만료
        PasswordResetToken token = PasswordResetToken.create(userId, code, expiresAt);
        if (used) {
            token.use(LocalDateTime.now());
        }
        return token;
    }

    // 1. requestReset — 비밀번호 재설정 요청 (Step 1)

    @Nested
    @DisplayName("비밀번호 재설정 요청 (requestReset)")
    class RequestReset {

        @Test
        @DisplayName("정상 요청 — 토큰 저장 후 이메일 발송")
        void requestResetSuccess() {
            // given
            PasswordResetRequest request = new PasswordResetRequest("testlogin", "user@test.com");
            stubUserAndLocalAuth();
            when(tokenRepository.existsByToken(anyString())).thenReturn(false); // 코드 중복 없음
            when(properties.getTokenTtlMinutes()).thenReturn(30L);
            when(properties.getFromAddress()).thenReturn("noreply@lastcup.com");

            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // when
            passwordResetService.requestReset(request);

            // then — 토큰 저장 + 메일 발송 확인
            verify(tokenRepository).save(any(PasswordResetToken.class));
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 요청 시 예외")
        void requestResetUserNotFound() {
            // given
            PasswordResetRequest request = new PasswordResetRequest("testlogin", "nonexist@test.com");
            when(userRepository.findByEmail("nonexist@test.com")).thenReturn(Optional.empty());

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> passwordResetService.requestReset(request));

            assertEquals("user not found", ex.getMessage());
        }

        @Test
        @DisplayName("존재하지 않는 loginId로 요청 시 예외")
        void requestResetLocalAuthNotFound() {
            // given
            PasswordResetRequest request = new PasswordResetRequest("unknown", "user@test.com");
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
            when(localAuthRepository.findByLoginId("unknown")).thenReturn(Optional.empty());

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> passwordResetService.requestReset(request));

            assertEquals("local auth not found", ex.getMessage());
        }

        @Test
        @DisplayName("loginId와 email이 다른 유저에 속할 때 예외")
        void requestResetMismatch() {
            // given — email은 userId=1, loginId는 userId=999 → 다른 유저
            // 이 교차 검증이 없으면 타인의 비밀번호를 재설정할 수 있는 보안 취약점 발생
            PasswordResetRequest request = new PasswordResetRequest("otherlogin", "user@test.com");
            LocalAuth otherAuth = LocalAuth.create(999L, "otherlogin", "hash");

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
            when(localAuthRepository.findByLoginId("otherlogin")).thenReturn(Optional.of(otherAuth));

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> passwordResetService.requestReset(request));

            assertEquals("loginId and email mismatch", ex.getMessage());
        }

        @Test
        @DisplayName("메일 발송 실패 시 예외 전파")
        void requestResetMailFailure() {
            // given
            PasswordResetRequest request = new PasswordResetRequest("testlogin", "user@test.com");
            stubUserAndLocalAuth();
            when(tokenRepository.existsByToken(anyString())).thenReturn(false);
            when(properties.getTokenTtlMinutes()).thenReturn(30L);

            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            // sendResetMail()은 MessagingException만 catch → IllegalStateException으로 감싸서 throw
            // RuntimeException은 MessagingException 하위가 아니므로 catch되지 않고 그대로 전파
            doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

            // when & then
            assertThrows(RuntimeException.class,
                    () -> passwordResetService.requestReset(request));
        }
    }

    // 2. verifyResetCode — 인증 코드 검증 (Step 2)
    // 이 단계는 코드가 유효한지만 확인하고, 상태를 변경하지 않는다.
    // 클라이언트가 "코드 입력 → 검증 → 새 비밀번호 입력" UI 플로우를 위해 필요.

    @Nested
    @DisplayName("인증 코드 검증 (verifyResetCode)")
    class VerifyResetCode {

        @Test
        @DisplayName("유효한 코드로 검증 성공 — 상태 변경 없음")
        void verifyResetCodeSuccess() {
            // given
            PasswordResetVerifyRequest request =
                    new PasswordResetVerifyRequest("testlogin", "user@test.com", "AB3CD");
            stubUserAndLocalAuth();

            PasswordResetToken validToken = createToken(1L, "AB3CD", false, false);
            when(tokenRepository.findByToken("AB3CD")).thenReturn(Optional.of(validToken));

            // when & then — 예외 없이 통과
            assertDoesNotThrow(() -> passwordResetService.verifyResetCode(request));

            // verify 단계에서는 token.use()가 호출되지 않아야 한다
            // (상태 변경은 confirmReset에서만 수행)
            assertNull(validToken.getUsedAt());
        }

        @Test
        @DisplayName("소문자 코드 입력 시 대문자로 변환하여 조회")
        void verifyResetCodeCaseInsensitive() {
            // given — 사용자가 소문자 "ab3cd"를 입력해도 DB에서는 "AB3CD"로 조회
            // getValidToken() 내부에서 verificationCode.toUpperCase(Locale.ROOT) 수행
            PasswordResetVerifyRequest request =
                    new PasswordResetVerifyRequest("testlogin", "user@test.com", "ab3cd");
            stubUserAndLocalAuth();

            PasswordResetToken validToken = createToken(1L, "AB3CD", false, false);
            when(tokenRepository.findByToken("AB3CD")).thenReturn(Optional.of(validToken));

            // when & then
            assertDoesNotThrow(() -> passwordResetService.verifyResetCode(request));
        }

        @Test
        @DisplayName("만료된 코드로 검증 시 예외")
        void verifyResetCodeExpired() {
            // given — expiresAt이 현재 시각보다 이전인 토큰
            PasswordResetVerifyRequest request =
                    new PasswordResetVerifyRequest("testlogin", "user@test.com", "EXP12");
            stubUserAndLocalAuth();

            PasswordResetToken expiredToken = createToken(1L, "EXP12", true, false);
            when(tokenRepository.findByToken("EXP12")).thenReturn(Optional.of(expiredToken));

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> passwordResetService.verifyResetCode(request));

            assertEquals("password reset code invalid", ex.getMessage());
        }

        @Test
        @DisplayName("이미 사용된 코드로 검증 시 예외")
        void verifyResetCodeAlreadyUsed() {
            // given — usedAt이 설정된 토큰 (1회용 코드 재사용 방지)
            PasswordResetVerifyRequest request =
                    new PasswordResetVerifyRequest("testlogin", "user@test.com", "USED1");
            stubUserAndLocalAuth();

            PasswordResetToken usedToken = createToken(1L, "USED1", false, true);
            when(tokenRepository.findByToken("USED1")).thenReturn(Optional.of(usedToken));

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> passwordResetService.verifyResetCode(request));

            assertEquals("password reset code invalid", ex.getMessage());
        }

        @Test
        @DisplayName("존재하지 않는 코드로 검증 시 예외")
        void verifyResetCodeNotFound() {
            // given
            PasswordResetVerifyRequest request =
                    new PasswordResetVerifyRequest("testlogin", "user@test.com", "XXXXX");
            stubUserAndLocalAuth();

            when(tokenRepository.findByToken("XXXXX")).thenReturn(Optional.empty());

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> passwordResetService.verifyResetCode(request));

            assertEquals("password reset code not found", ex.getMessage());
        }

        @Test
        @DisplayName("다른 유저의 토큰으로 검증 시 예외 — userId 불일치")
        void verifyResetCodeWrongUser() {
            // given — 토큰은 존재하지만 userId가 다름
            // getValidToken()에서 filter(found -> found.getUserId().equals(user.getId()))로 걸러짐
            PasswordResetVerifyRequest request =
                    new PasswordResetVerifyRequest("testlogin", "user@test.com", "OTHER");
            stubUserAndLocalAuth();

            PasswordResetToken otherUserToken = createToken(999L, "OTHER", false, false);
            when(tokenRepository.findByToken("OTHER")).thenReturn(Optional.of(otherUserToken));

            // when & then — "not found"로 응답 (보안상 "wrong user" 노출 금지)
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> passwordResetService.verifyResetCode(request));

            assertEquals("password reset code not found", ex.getMessage());
        }
    }

    // 3. confirmReset — 비밀번호 확정 변경 (Step 3)

    @Nested
    @DisplayName("비밀번호 확정 변경 (confirmReset)")
    class ConfirmReset {

        @Test
        @DisplayName("정상 확정 — 비밀번호 변경 후 토큰 사용처리")
        void confirmResetSuccess() {
            // given
            PasswordResetConfirmRequest request =
                    new PasswordResetConfirmRequest("testlogin", "user@test.com", "VALID", "newPassword123!");
            stubUserAndLocalAuth();

            PasswordResetToken validToken = createToken(1L, "VALID", false, false);
            when(tokenRepository.findByToken("VALID")).thenReturn(Optional.of(validToken));
            when(localAuthRepository.findById(1L)).thenReturn(Optional.of(testLocalAuth));
            when(passwordEncoder.encode("newPassword123!")).thenReturn("new-encoded-hash");

            // when
            passwordResetService.confirmReset(request);

            // then — 비밀번호 변경 + 토큰 사용 처리 (상태 검증)
            assertEquals("new-encoded-hash", testLocalAuth.getPasswordHash());
            assertNotNull(validToken.getUsedAt()); // token.use(now)가 호출되어 usedAt이 설정됨
        }

        @Test
        @DisplayName("만료된 토큰으로 확정 시 예외")
        void confirmResetExpired() {
            // given
            PasswordResetConfirmRequest request =
                    new PasswordResetConfirmRequest("testlogin", "user@test.com", "EXPRD", "newPassword123!");
            stubUserAndLocalAuth();

            PasswordResetToken expiredToken = createToken(1L, "EXPRD", true, false);
            when(tokenRepository.findByToken("EXPRD")).thenReturn(Optional.of(expiredToken));

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> passwordResetService.confirmReset(request));

            assertEquals("password reset code invalid", ex.getMessage());
            // 만료된 토큰이면 비밀번호 변경 로직에 진입하면 안 됨
            verify(localAuthRepository, never()).findById(any());
        }

        @Test
        @DisplayName("이미 사용된 토큰으로 확정 시 예외")
        void confirmResetAlreadyUsed() {
            // given
            PasswordResetConfirmRequest request =
                    new PasswordResetConfirmRequest("testlogin", "user@test.com", "USEDT", "newPassword123!");
            stubUserAndLocalAuth();

            PasswordResetToken usedToken = createToken(1L, "USEDT", false, true);
            when(tokenRepository.findByToken("USEDT")).thenReturn(Optional.of(usedToken));

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> passwordResetService.confirmReset(request));

            assertEquals("password reset code invalid", ex.getMessage());
        }

        @Test
        @DisplayName("LocalAuth를 찾을 수 없을 때 예외 (비정상 데이터)")
        void confirmResetLocalAuthNotFound() {
            // given — 토큰은 유효하지만 LocalAuth가 삭제된 비정상 상태
            // 실무에서 소셜 로그인 전환 후 LocalAuth가 제거된 경우 발생 가능
            PasswordResetConfirmRequest request =
                    new PasswordResetConfirmRequest("testlogin", "user@test.com", "NOLOC", "newPassword123!");
            stubUserAndLocalAuth();

            PasswordResetToken validToken = createToken(1L, "NOLOC", false, false);
            when(tokenRepository.findByToken("NOLOC")).thenReturn(Optional.of(validToken));
            when(localAuthRepository.findById(1L)).thenReturn(Optional.empty());

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> passwordResetService.confirmReset(request));

            assertEquals("local auth not found", ex.getMessage());
        }
    }
}
