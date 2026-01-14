package com.lastcup.api.infrastructure.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class GoogleClient implements OAuthTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleClient(@Value("${app.oauth.google.client-ids}") String clientIds) {
        List<String> audiences = parseClientIds(clientIds);
        this.verifier = createVerifier(audiences);
    }

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public VerifiedOAuthUser verify(String providerAccessToken) {
        GoogleIdToken idToken = verifyIdToken(providerAccessToken);
        GoogleIdToken.Payload payload = idToken.getPayload();

        String providerUserKey = payload.getSubject();
        String email = payload.getEmail();
        String picture = (String) payload.get("picture");

        validateProviderUserKey(providerUserKey);
        return new VerifiedOAuthUser(providerUserKey, email, picture);
    }

    private GoogleIdToken verifyIdToken(String token) {
        try {
            GoogleIdToken idToken = verifier.verify(token);
            if (idToken != null) {
                return idToken;
            }
        } catch (Exception ignored) {
        }
        throw new OAuthVerificationException("GOOGLE_ID_TOKEN_INVALID");
    }

    private GoogleIdTokenVerifier createVerifier(List<String> audiences) {
        return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                .setAudience(audiences)
                .build();
    }

    private List<String> parseClientIds(String clientIds) {
        if (clientIds == null || clientIds.isBlank()) {
            throw new OAuthVerificationException("GOOGLE_CLIENT_IDS_EMPTY");
        }
        return Arrays.stream(clientIds.split(","))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .toList();
    }

    private void validateProviderUserKey(String providerUserKey) {
        if (providerUserKey == null || providerUserKey.isBlank()) {
            throw new OAuthVerificationException("GOOGLE_SUB_EMPTY");
        }
    }
}
