package com.lastcup.api.infrastructure.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoClient {

    private final RestClient restClient;
    private final String tokenUrl;
    private final String userInfoUrl;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoClient(
            @Value("${app.oauth.kakao.token-url:https://kauth.kakao.com/oauth/token}") String tokenUrl,
            @Value("${app.oauth.kakao.user-info-url:https://kapi.kakao.com/v2/user/me?secure_resource=true}") String userInfoUrl,
            @Value("${app.oauth.kakao.client-id:}") String clientId,
            @Value("${app.oauth.kakao.client-secret:}") String clientSecret,
            @Value("${app.oauth.kakao.redirect-uri:}") String redirectUri
    ) {
        this.restClient = RestClient.builder().build();
        this.tokenUrl = tokenUrl;
        this.userInfoUrl = userInfoUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public VerifiedOAuthUser verifyAuthorizationCode(String authorizationCode) {
        validateAuthorizationCode(authorizationCode);
        validateClientId();

        KakaoTokenResponse token = fetchAccessToken(authorizationCode);
        KakaoUserResponse userInfo = fetchUserInfo(token.accessToken);

        String providerUserKey = extractProviderUserKey(userInfo);
        String email = extractEmail(userInfo);
        String profileImageUrl = extractProfileImageUrl(userInfo);

        validateProviderUserKey(providerUserKey);
        return new VerifiedOAuthUser(providerUserKey, email, profileImageUrl);
    }

    private void validateAuthorizationCode(String code) {
        if (code == null || code.isBlank()) {
            throw new OAuthVerificationException("KAKAO_AUTHORIZATION_CODE_EMPTY");
        }
    }

    private void validateClientId() {
        if (clientId == null || clientId.isBlank()) {
            throw new OAuthVerificationException("KAKAO_CLIENT_ID_EMPTY");
        }
    }

    private KakaoTokenResponse fetchAccessToken(String code) {
        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(createTokenRequest(code))
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response != null && response.accessToken != null && !response.accessToken.isBlank()) {
                return response;
            }
        } catch (RestClientException ignored) {
        }
        throw new OAuthVerificationException("KAKAO_TOKEN_EXCHANGE_FAILED");
    }

    private MultiValueMap<String, String> createTokenRequest(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("code", code);
        if (redirectUri != null && !redirectUri.isBlank()) {
            form.add("redirect_uri", redirectUri);
        }
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }
        return form;
    }

    private KakaoUserResponse fetchUserInfo(String accessToken) {
        try {
            KakaoUserResponse response = restClient.get()
                    .uri(userInfoUrl)
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
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

    private String extractProviderUserKey(KakaoUserResponse userInfo) {
        if (userInfo.id == null) {
            throw new OAuthVerificationException("KAKAO_ID_MISSING");
        }
        return String.valueOf(userInfo.id);
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
    static class KakaoTokenResponse {
        @JsonProperty("access_token")
        public String accessToken;
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
