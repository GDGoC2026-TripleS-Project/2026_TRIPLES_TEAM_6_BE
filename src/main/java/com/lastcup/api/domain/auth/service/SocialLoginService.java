package com.lastcup.api.domain.auth.service;

import com.lastcup.api.domain.auth.dto.request.SocialLoginRequest;
import com.lastcup.api.domain.auth.dto.response.AuthResponse;
import com.lastcup.api.domain.auth.dto.response.AuthTokensResponse;
import com.lastcup.api.domain.auth.dto.response.UserSummaryResponse;
import com.lastcup.api.domain.user.domain.SocialAuth;
import com.lastcup.api.domain.user.domain.User;
import com.lastcup.api.domain.user.repository.SocialAuthRepository;
import com.lastcup.api.domain.user.repository.UserRepository;
import com.lastcup.api.infrastructure.oauth.KakaoClient;
import com.lastcup.api.infrastructure.oauth.AppleClient;
import com.lastcup.api.infrastructure.oauth.OAuthTokenVerifier;
import com.lastcup.api.infrastructure.oauth.SocialProvider;
import com.lastcup.api.infrastructure.oauth.VerifiedOAuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SocialLoginService {

    private final List<OAuthTokenVerifier> verifiers;
    private final KakaoClient kakaoClient;
    private final AppleClient appleClient;
    private final UserRepository userRepository;
    private final SocialAuthRepository socialAuthRepository;
    private final NicknameGenerator nicknameGenerator;
    private final TokenService tokenService;

    public SocialLoginService(
            List<OAuthTokenVerifier> verifiers,
            KakaoClient kakaoClient,
            AppleClient appleClient,
            UserRepository userRepository,
            SocialAuthRepository socialAuthRepository,
            NicknameGenerator nicknameGenerator,
            TokenService tokenService
    ) {
        this.verifiers = verifiers;
        this.kakaoClient = kakaoClient;
        this.appleClient = appleClient;
        this.userRepository = userRepository;
        this.socialAuthRepository = socialAuthRepository;
        this.nicknameGenerator = nicknameGenerator;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse login(SocialProvider provider, SocialLoginRequest request) {
        VerifiedOAuthUser verified = verifyWithProvider(provider, request);
        return loginWithVerified(provider, verified);
    }

    private VerifiedOAuthUser verifyWithProvider(SocialProvider provider, SocialLoginRequest request) {
        if (provider == SocialProvider.KAKAO) {
            return kakaoClient.verifyAuthorizationCode(extractAuthorizationCode(request));
        }
        if (provider == SocialProvider.APPLE) {
            return appleClient.verifyAuthorizationCode(
                    extractAuthorizationCode(request),
                    request.identityToken()
            );
        }
        return findVerifier(provider).verify(extractProviderAccessToken(request));
    }

    private String extractAuthorizationCode(SocialLoginRequest request) {
        String code = request.authorizationCode();
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("authorizationCode is required");
        }
        return code;
    }

    private String extractProviderAccessToken(SocialLoginRequest request) {
        String token = request.providerAccessToken();
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("providerAccessToken is required");
        }
        return token;
    }

    private AuthResponse loginWithVerified(SocialProvider provider, VerifiedOAuthUser verified) {
        return socialAuthRepository.findByProviderAndProviderUserKey(provider, verified.providerUserKey())
                .map(this::loginExistingUser)
                .orElseGet(() -> signupNewUser(provider, verified));
    }

    private AuthResponse loginExistingUser(SocialAuth socialAuth) {
        User user = userRepository.findById(socialAuth.getUserId())
                .orElseThrow(() -> new IllegalStateException("user not found"));

        AuthTokensResponse tokens = tokenService.createTokens(user.getId());
        return new AuthResponse(new UserSummaryResponse(user.getId(), user.getNickname()), tokens, false);
    }

    private AuthResponse signupNewUser(SocialProvider provider, VerifiedOAuthUser verified) {
        User user = userRepository.save(User.create(createUniqueNickname(), verified.profileImageUrl()));
        socialAuthRepository.save(SocialAuth.create(
                user.getId(),
                provider,
                verified.providerUserKey(),
                verified.email()
        ));

        AuthTokensResponse tokens = tokenService.createTokens(user.getId());
        return new AuthResponse(new UserSummaryResponse(user.getId(), user.getNickname()), tokens, true);
    }

    private String createUniqueNickname() {
        for (int i = 0; i < 20; i++) {
            String nickname = nicknameGenerator.create();
            if (!userRepository.existsByNickname(nickname)) {
                return nickname;
            }
        }
        throw new IllegalStateException("nickname create failed");
    }

    private OAuthTokenVerifier findVerifier(SocialProvider provider) {
        return verifiers.stream()
                .filter(v -> v.getProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported provider"));
    }
}
