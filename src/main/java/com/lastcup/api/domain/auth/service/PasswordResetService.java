package com.lastcup.api.domain.auth.service;

import static com.lastcup.api.global.config.AppTimeZone.KST;

import com.lastcup.api.domain.auth.config.PasswordResetProperties;
import com.lastcup.api.domain.auth.domain.PasswordResetToken;
import com.lastcup.api.domain.auth.dto.request.PasswordResetConfirmRequest;
import com.lastcup.api.domain.auth.dto.request.PasswordResetRequest;
import com.lastcup.api.domain.auth.repository.PasswordResetTokenRepository;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final LocalAuthRepository localAuthRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final PasswordResetProperties properties;

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

        localAuthRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("local auth not found"));

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now(KST).plusMinutes(properties.getTokenTtlMinutes());
        tokenRepository.save(PasswordResetToken.create(user.getId(), token, expiresAt));

        sendResetMail(request.email(), buildResetLink(token));
    }

    @Transactional
    public void confirmReset(PasswordResetConfirmRequest request) {
        PasswordResetToken token = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("password reset token not found"));

        LocalDateTime now = LocalDateTime.now(KST);
        if (token.isUsed() || token.isExpired(now)) {
            throw new IllegalArgumentException("password reset token invalid");
        }

        LocalAuth localAuth = localAuthRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("local auth not found"));
        localAuth.updatePasswordHash(passwordEncoder.encode(request.newPassword()));

        token.use(now);
    }

    private String buildResetLink(String token) {
        String baseUrl = Objects.requireNonNullElse(properties.getBaseUrl(), "");
        String delimiter = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + delimiter + "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private void sendResetMail(String email, String link) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

            helper.setTo(email);

            String fromAddress = properties.getFromAddress();
            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(fromAddress);
            }

            helper.setSubject("비밀번호 재설정 안내");

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
                                    비밀번호 재설정 안내
                                  </h2>
                                </td>
                              </tr>
                    
                              <!-- 본문 -->
                              <tr>
                                <td style="padding: 16px 0 24px; text-align: center; color: #374151; font-size: 15px; line-height: 1.6;">
                                  아래 버튼을 클릭하면<br/>
                                  새로운 비밀번호를 설정할 수 있습니다.
                                </td>
                              </tr>
                    
                              <!-- 버튼 -->
                              <tr>
                                <td align="center">
                                  <a href="%s"
                                     style="
                                       display: inline-block;
                                       padding: 14px 28px;
                                       background-color: #2563eb;
                                       color: #ffffff;
                                       text-decoration: none;
                                       border-radius: 10px;
                                       font-size: 15px;
                                       font-weight: 600;
                                     ">
                                    비밀번호 재설정
                                  </a>
                                </td>
                              </tr>
                    
                              <!-- 안내 문구 -->
                              <tr>
                                <td style="padding-top: 28px; text-align: center; font-size: 12px; color: #6b7280; line-height: 1.6;">
                                  본인이 요청하지 않았다면 이 메일을 무시해주세요.<br/>
                                  이 링크는 <strong>%d분</strong> 동안만 유효합니다.
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
                    """.formatted(link, properties.getTokenTtlMinutes());


            helper.setText(html, true); // true = HTML 메일
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new IllegalStateException("mail send failed", e);
        }
    }

}
