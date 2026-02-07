package com.lastcup.api.domain.auth.service;

import com.lastcup.api.domain.auth.dto.request.SocialLoginRequest;
import com.lastcup.api.domain.auth.dto.response.AuthResponse;
import com.lastcup.api.domain.auth.dto.response.AuthTokensResponse;
import com.lastcup.api.domain.auth.dto.response.UserSummaryResponse;
import com.lastcup.api.domain.user.domain.SocialAuth;
import com.lastcup.api.domain.user.domain.User;
import com.lastcup.api.domain.user.repository.SocialAuthRepository;
import com.lastcup.api.domain.user.repository.UserRepository;
import com.lastcup.api.infrastructure.oauth.OAuthTokenVerifier;
import com.lastcup.api.infrastructure.oauth.SocialProvider;
import com.lastcup.api.infrastructure.oauth.VerifiedOAuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SocialLoginService {

    private static final Logger log = LoggerFactory.getLogger(SocialLoginService.class);

    private final List<OAuthTokenVerifier> verifiers;
    private final UserRepository userRepository;
    private final SocialAuthRepository socialAuthRepository;
    private final NicknameGenerator nicknameGenerator;
    private final TokenService tokenService;

    public SocialLoginService(
            List<OAuthTokenVerifier> verifiers,
            UserRepository userRepository,
            SocialAuthRepository socialAuthRepository,
            NicknameGenerator nicknameGenerator,
            TokenService tokenService
    ) {
        this.verifiers = verifiers;
        this.userRepository = userRepository;
        this.socialAuthRepository = socialAuthRepository;
        this.nicknameGenerator = nicknameGenerator;
        this.tokenService = tokenService;
    }

    /**
     * 소셜 로그인 통합 플로우.
     *
     * <h3>Apple 이메일 처리 전략</h3>
     * Apple은 최초 인가 1회에만 이메일을 제공하고, 이후에는 절대 재전달하지 않는다.
     * <ul>
     *   <li>ID Token 안의 email claim → 최초 인가 시에만 존재</li>
     *   <li>iOS client의 ASAuthorizationAppleIDCredential.email → 최초 인가 시에만 존재</li>
     * </ul>
     * 따라서 iOS 클라이언트는 Apple 최초 인가 시 받은 email을
     * {@code SocialLoginRequest.email}에 반드시 포함해서 전송해야 한다.
     *
     * <p>이 메서드는 다음 순서로 이메일을 결정한다:</p>
     * <ol>
     *   <li>ID Token에서 추출한 email (Apple 최초 인가 시)</li>
     *   <li>클라이언트가 request body에 보낸 email (Apple 최초 인가 시 fallback)</li>
     * </ol>
     *
     * <p>기존 유저가 이메일 없이 가입된 경우(첫 가입 실패 후 재시도 등),
     * 이번 요청에 이메일이 포함되어 있으면 보충 업데이트한다.</p>
     */
    @Transactional
    public AuthResponse login(SocialProvider provider, SocialLoginRequest request) {
        OAuthTokenVerifier verifier = findVerifier(provider);

        VerifiedOAuthUser verified = verifier.verify(request.providerToken());

        // 이메일 우선순위: ID Token claim > 클라이언트 request body
        String email = verified.email() != null
                ? verified.email()
                : request.email();

        VerifiedOAuthUser resolved = new VerifiedOAuthUser(
                verified.providerUserKey(),
                email,
                verified.profileImageUrl()
        );

        return socialAuthRepository
                .findByProviderAndProviderUserKey(provider, resolved.providerUserKey())
                .map(socialAuth -> loginExistingUser(socialAuth, resolved.email()))
                .orElseGet(() -> signupNewUser(provider, resolved));
    }

    private OAuthTokenVerifier findVerifier(SocialProvider provider) {
        return verifiers.stream()
                .filter(v -> v.getProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported provider"));
    }

    /**
     * 기존 유저 로그인.
     * <p>이메일이 없는 상태로 가입된 유저(Apple 첫 가입 실패 후 재시도 등)에게
     * 이번 요청에 이메일이 포함되어 있으면 User + SocialAuth 양쪽에 보충한다.</p>
     */
    private AuthResponse loginExistingUser(SocialAuth socialAuth, String email) {
        User user = userRepository.findById(socialAuth.getUserId())
                .orElseThrow(() -> new IllegalStateException("user not found"));

        // Apple 이메일 보충: 기존에 이메일 없이 가입된 경우 업데이트
        if (email != null && !email.isBlank()) {
            boolean userUpdated = user.updateEmailIfAbsent(email);
            boolean authUpdated = socialAuth.updateEmailIfAbsent(email);
            if (userUpdated || authUpdated) {
                log.info("소셜 로그인 이메일 보충 완료: userId={}, provider={}",
                        user.getId(), socialAuth.getProvider());
            }
        }

        AuthTokensResponse tokens = tokenService.createTokens(user.getId());
        return new AuthResponse(
                new UserSummaryResponse(user.getId(), user.getNickname()),
                tokens,
                false
        );
    }

    private AuthResponse signupNewUser(SocialProvider provider, VerifiedOAuthUser verified) {
        // 이메일 중복 체크 (null이면 skip — 이메일 없이 가입 허용)
        if (verified.email() != null && !verified.email().isBlank()
                && userRepository.existsByEmail(verified.email())) {
            throw new IllegalArgumentException("email already exists");
        }

        User user = userRepository.save(
                User.create(
                        nicknameGenerator.create(),
                        verified.email(),
                        verified.profileImageUrl()
                )
        );

        socialAuthRepository.save(
                SocialAuth.create(
                        user.getId(),
                        provider,
                        verified.providerUserKey(),
                        verified.email()
                )
        );

        AuthTokensResponse tokens = tokenService.createTokens(user.getId());
        return new AuthResponse(
                new UserSummaryResponse(user.getId(), user.getNickname()),
                tokens,
                true
        );
    }
}
