package com.lastcup.api.infrastructure.oauth;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AppleClient implements OAuthTokenVerifier {

    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final Logger log = LoggerFactory.getLogger(AppleClient.class);

    private final RestClient restClient = RestClient.builder().build();
    private final String keyUrl;
    private final String clientId;

    public AppleClient(
            @Value("${app.oauth.apple.key-url}") String keyUrl,
            @Value("${app.oauth.apple.client-id}") String clientId
    ) {
        this.keyUrl = keyUrl;
        this.clientId = clientId;
    }

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.APPLE;
    }

    @Override
    public VerifiedOAuthUser verify(String idToken) {
        try {
            if (clientId == null || clientId.isBlank()) {
                throw new OAuthVerificationException("APPLE_CLIENT_ID_EMPTY");
            }

            SignedJWT signedJWT = SignedJWT.parse(idToken);
            JWSHeader header = signedJWT.getHeader();

            String keyId = header.getKeyID();
            RSAKey key = fetchKey(keyId);
            JWSVerifier verifier = new RSASSAVerifier(key.toRSAPublicKey());

            if (!signedJWT.verify(verifier)) {
                throw new OAuthVerificationException("APPLE_ID_TOKEN_INVALID");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            Date expiresAt = claims.getExpirationTime();
            if (expiresAt != null && expiresAt.before(new Date())) {
                throw new OAuthVerificationException("APPLE_ID_TOKEN_EXPIRED");
            }

            if (!APPLE_ISSUER.equals(claims.getIssuer())) {
                throw new OAuthVerificationException("APPLE_ID_TOKEN_ISSUER_INVALID");
            }

            if (!claims.getAudience().contains(clientId)) {
                throw new OAuthVerificationException("APPLE_ID_TOKEN_AUDIENCE_INVALID");
            }

            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new OAuthVerificationException("APPLE_SUB_EMPTY");
            }

            return new VerifiedOAuthUser(
                    subject,
                    claims.getStringClaim("email"),
                    null
            );
        } catch (OAuthVerificationException ex) {
            log.warn("Apple ID Token 검증 실패: code={}, message={}", ex.getMessage(), ex.getMessage());
            throw ex;
        } catch (Exception e) {
            log.error("Apple ID Token 검증 중 예외 발생: {}", e.getMessage(), e);
            throw new OAuthVerificationException("APPLE_ID_TOKEN_INVALID");
        }
    }

    private RSAKey fetchKey(String keyId) {
        try {
            if (keyId == null || keyId.isBlank()) {
                throw new OAuthVerificationException("APPLE_JWK_NOT_FOUND");
            }

            String jwkJson = restClient.get()
                    .uri(keyUrl)
                    .retrieve()
                    .body(String.class);

            JWKSet jwkSet = JWKSet.parse(jwkJson);
            JWK jwk = jwkSet.getKeyByKeyId(keyId);

            if (jwk == null) {
                throw new OAuthVerificationException("APPLE_JWK_NOT_FOUND");
            }
            if (!(jwk instanceof RSAKey rsaKey)) {
                throw new OAuthVerificationException("APPLE_JWK_TYPE_INVALID");
            }
            return rsaKey;
        } catch (OAuthVerificationException ex) {
            throw ex;
        } catch (Exception e) {
            throw new OAuthVerificationException("APPLE_JWK_FETCH_FAILED");
        }
    }
}
