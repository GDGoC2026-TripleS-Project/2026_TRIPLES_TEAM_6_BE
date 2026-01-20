package com.lastcup.api.infrastructure.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoClient implements OAuthTokenVerifier {

    private final RestClient restClient;
    private final String accessTokenInfoUrl;
    private final String userInfoUrl;
    private final String expectedAppId;

    public KakaoClient(
            @Value("${app.oauth.kakao.access-token-info-url:https://kapi.kakao.com/v1/user/access_token_info}") String accessTokenInfoUrl,
            @Value("${app.oauth.kakao.user-info-url:https://kapi.kakao.com/v2/user/me?secure_resource=true}") String userInfoUrl,
            @Value("${app.oauth.kakao.app-id:}") String expectedAppId
    ) {
        this.restClient = RestClient.builder().build();
        this.accessTokenInfoUrl = accessTokenInfoUrl;
        this.userInfoUrl = userInfoUrl;
        this.expectedAppId = expectedAppId;
    }

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public VerifiedOAuthUser verify(String providerAccessToken) {
        validateToken(providerAccessToken);

        KakaoTokenInfoResponse tokenInfo = fetchTokenInfo(providerAccessToken);
        validateAppIdIfConfigured(tokenInfo);

        KakaoUserResponse userInfo = fetchUserInfo(providerAccessToken);

        String providerUserKey = toProviderUserKey(tokenInfo);
        String email = extractEmail(userInfo);
        String profileImageUrl = extractProfileImageUrl(userInfo);

        validateProviderUserKey(providerUserKey);
        return new VerifiedOAuthUser(providerUserKey, email, profileImageUrl);
    }

    private void validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new OAuthVerificationException("KAKAO_ACCESS_TOKEN_EMPTY");
        }
    }

    private KakaoTokenInfoResponse fetchTokenInfo(String token) {
        try {
            KakaoTokenInfoResponse response = restClient.get()
                    .uri(accessTokenInfoUrl)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(KakaoTokenInfoResponse.class);

            if (response != null) {
                return response;
            }
        } catch (RestClientException ignored) {
        }
        throw new OAuthVerificationException("KAKAO_ACCESS_TOKEN_INVALID");
    }

    private KakaoUserResponse fetchUserInfo(String token) {
        try {
            KakaoUserResponse response = restClient.get()
                    .uri(userInfoUrl)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(KakaoUserResponse.class);

            if (response != null) {
                return response;
            }
        } catch (RestClientException ignored) {
        }
        throw new OAuthVerificationException("KAKAO_USER_INFO_FETCH_FAILED");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void validateAppIdIfConfigured(KakaoTokenInfoResponse tokenInfo) {
        if (expectedAppId == null || expectedAppId.isBlank()) {
            return;
        }
        if (tokenInfo.appId == null) {
            throw new OAuthVerificationException("KAKAO_APP_ID_MISSING");
        }
        if (!expectedAppId.equals(String.valueOf(tokenInfo.appId))) {
            throw new OAuthVerificationException("KAKAO_APP_ID_MISMATCH");
        }
    }

    private String toProviderUserKey(KakaoTokenInfoResponse tokenInfo) {
        if (tokenInfo.id == null) {
            throw new OAuthVerificationException("KAKAO_ID_MISSING");
        }
        return String.valueOf(tokenInfo.id);
    }

    private String extractEmail(KakaoUserResponse userInfo) {
        if (userInfo.kakaoAccount == null) {
            return null;
        }
        return userInfo.kakaoAccount.email;
    }

    private String extractProfileImageUrl(KakaoUserResponse userInfo) {
        String fromAccount = extractFromAccountProfile(userInfo);
        if (fromAccount != null && !fromAccount.isBlank()) {
            return fromAccount;
        }
        return extractFromProperties(userInfo);
    }

    private String extractFromAccountProfile(KakaoUserResponse userInfo) {
        if (userInfo.kakaoAccount == null) {
            return null;
        }
        if (userInfo.kakaoAccount.profile == null) {
            return null;
        }
        return userInfo.kakaoAccount.profile.profileImageUrl;
    }

    private String extractFromProperties(KakaoUserResponse userInfo) {
        if (userInfo.properties == null) {
            return null;
        }
        return userInfo.properties.profileImage;
    }

    private void validateProviderUserKey(String providerUserKey) {
        if (providerUserKey == null || providerUserKey.isBlank()) {
            throw new OAuthVerificationException("KAKAO_PROVIDER_USER_KEY_EMPTY");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class KakaoTokenInfoResponse {
        @JsonProperty("id")
        public Long id;

        @JsonProperty("app_id")
        public Long appId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class KakaoUserResponse {
        @JsonProperty("id")
        public Long id;

        @JsonProperty("properties")
        public KakaoProperties properties;

        @JsonProperty("kakao_account")
        public KakaoAccount kakaoAccount;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class KakaoProperties {
        @JsonProperty("profile_image")
        public String profileImage;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class KakaoAccount {
        @JsonProperty("email")
        public String email;

        @JsonProperty("profile")
        public KakaoProfile profile;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class KakaoProfile {
        @JsonProperty("profile_image_url")
        public String profileImageUrl;
    }
}
