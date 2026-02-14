package com.lastcup.api.domain.auth.service;

import static com.lastcup.api.global.config.AppTimeZone.KST;

import com.lastcup.api.domain.auth.config.PasswordResetProperties;
import com.lastcup.api.domain.auth.domain.PasswordResetToken;
import com.lastcup.api.domain.auth.dto.request.PasswordResetConfirmRequest;
import com.lastcup.api.domain.auth.dto.request.PasswordResetRequest;
import com.lastcup.api.domain.auth.repository.PasswordResetTokenRepository;
import com.lastcup.api.domain.auth.dto.request.PasswordResetVerifyRequest;
import com.lastcup.api.domain.user.domain.LocalAuth;
import com.lastcup.api.domain.user.domain.User;
import com.lastcup.api.domain.user.repository.LocalAuthRepository;
import com.lastcup.api.domain.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class PasswordResetService {

    private static final int VERIFICATION_CODE_LENGTH = 5;
    private static final String VERIFICATION_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final UserRepository userRepository;
    private final LocalAuthRepository localAuthRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final PasswordResetProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            LocalAuthRepository localAuthRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            PasswordResetProperties properties
    ) {
        this.userRepository = userRepository;
        this.localAuthRepository = localAuthRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Transactional
    public void requestReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        LocalAuth localAuth = localAuthRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new IllegalArgumentException("local auth not found"));

        if (!user.getId().equals(localAuth.getUserId())) {
            throw new IllegalArgumentException("loginId and email mismatch");
        }

        String verificationCode = generateVerificationCode();
        LocalDateTime expiresAt = LocalDateTime.now(KST).plusMinutes(properties.getTokenTtlMinutes());
        tokenRepository.save(PasswordResetToken.create(user.getId(), verificationCode, expiresAt));

        sendResetMail(request.email(), verificationCode);
    }

    @Transactional(readOnly = true)
    public void verifyResetCode(PasswordResetVerifyRequest request) {
        PasswordResetToken token = getValidToken(request.loginId(), request.email(), request.verificationCode());
        LocalDateTime now = LocalDateTime.now(KST);
        if (token.isUsed() || token.isExpired(now)) {
            throw new IllegalArgumentException("password reset code invalid");
        }
    }


    @Transactional
    public void confirmReset(PasswordResetConfirmRequest request) {
        PasswordResetToken token = getValidToken(request.loginId(), request.email(), request.verificationCode());

        LocalDateTime now = LocalDateTime.now(KST);
        if (token.isUsed() || token.isExpired(now)) {
            throw new IllegalArgumentException("password reset code invalid");
        }

        LocalAuth localAuth = localAuthRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("local auth not found"));
        localAuth.updatePasswordHash(passwordEncoder.encode(request.newPassword()));

        token.use(now);
    }

    private PasswordResetToken getValidToken(String loginId, String email, String verificationCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        LocalAuth localAuth = localAuthRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("local auth not found"));

        if (!user.getId().equals(localAuth.getUserId())) {
            throw new IllegalArgumentException("loginId and email mismatch");
        }

        return tokenRepository.findByToken(verificationCode.toUpperCase(Locale.ROOT))
                .filter(found -> found.getUserId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("password reset code not found"));
    }

    private String generateVerificationCode() {
        StringBuilder builder = new StringBuilder(VERIFICATION_CODE_LENGTH);
        for (int i = 0; i < VERIFICATION_CODE_LENGTH; i++) {
            int index = secureRandom.nextInt(VERIFICATION_CODE_CHARS.length());
            builder.append(VERIFICATION_CODE_CHARS.charAt(index));
        }

        String code = builder.toString();
        if (tokenRepository.existsByToken(code)) {
            return generateVerificationCode();
        }
        return code;
    }

    private void sendResetMail(String email, String verificationCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

            helper.setTo(email);

            String fromAddress = properties.getFromAddress();
            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(fromAddress);
            }

            helper.setSubject("비밀번호 재설정 인증 코드 안내");

            String html = """
                    <html>
                    <body style="
                        margin: 0;
                        padding: 0;
                        background-color: #f4f6f8;
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                    ">
                      <table width="100%%" cellpadding="0" cellspacing="0">
                        <tr>
                          <td align="center" style="padding: 40px 0;">
                            <table width="100%%" style="max-width: 480px; background-color: #ffffff; border-radius: 12px; padding: 32px;">
                    
                              <!-- 제목 -->
                              <tr>
                                <td style="text-align: center;">
                                  <h2 style="margin: 0; color: #111827;">
                                    비밀번호 재설정 인증 코드
                                  </h2>
                                </td>
                              </tr>
                    
                              <!-- 본문 -->
                              <tr>
                                <td style="padding: 16px 0 24px; text-align: center; color: #374151; font-size: 15px; line-height: 1.6;">
                                  아래 인증 코드를 입력해 비밀번호 재설정을 계속 진행해주세요.
                                </td>
                              </tr>
                    
                              <!-- 버튼 -->
                              <tr>
                                <td style="text-align: center; padding: 8px 0 16px;">
                                                                        <div style="
                                                                            display: inline-block;
                                                                            letter-spacing: 6px;
                                                                            font-size: 30px;
                                                                            font-weight: 700;
                                                                            color: #2563eb;
                                                                            background-color: #eff6ff;
                                                                            padding: 14px 22px;
                                                                            border-radius: 10px;
                                                                        ">%s</div>
                                </td>
                              </tr>
                    
                              <!-- 안내 문구 -->
                              <tr>
                                <td style="padding-top: 10px; text-align: center; font-size: 12px; color: #6b7280; line-height: 1.6;">
                                                                       본인이 요청하지 않았다면 이 메일을 무시해주세요.<br/>
                                                                       인증 코드는 <strong>%d분</strong> 동안만 유효합니다.
                                                                     </td>
                              </tr>
                    
                            </table>
                    
                            <!-- 푸터 -->
                            <div style="margin-top: 16px; font-size: 11px; color: #9ca3af;">
                              © LastCup
                            </div>
                          </td>
                        </tr>
                      </table>
                    </body>
                    </html>
                    """.formatted(verificationCode, properties.getTokenTtlMinutes());


            helper.setText(html, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new IllegalStateException("mail send failed", e);
        }
    }

}
