package com.lastcup.api.infrastructure.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoClient implements OAuthTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(KakaoClient.class);

    private final RestClient restClient = RestClient.builder().build();
    private final String userInfoUrl;

    public KakaoClient(
            @Value("${app.oauth.kakao.user-info-url}") String userInfoUrl
    ) {
        this.userInfoUrl = userInfoUrl;
    }

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public VerifiedOAuthUser verify(String accessToken) {
        try {
            KakaoUserResponse response = restClient.get()
                    .uri(userInfoUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(KakaoUserResponse.class);

            if (response == null || response.id == null) {
                throw new OAuthVerificationException("KAKAO_ID_MISSING");
            }

            return new VerifiedOAuthUser(
                    response.id.toString(),
                    response.kakaoAccount != null ? response.kakaoAccount.email : null,
                    null
            );
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패: {}", e.getMessage(), e);
            throw new OAuthVerificationException("KAKAO_USER_INFO_FETCH_FAILED");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class KakaoUserResponse {
        @JsonProperty("id")
        public Long id;

        @JsonProperty("kakao_account")
        public KakaoAccount kakaoAccount;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class KakaoAccount {
        @JsonProperty("email")
        public String email;
    }
}
