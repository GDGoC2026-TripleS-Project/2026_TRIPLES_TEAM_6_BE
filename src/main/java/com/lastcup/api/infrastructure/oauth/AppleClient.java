package com.lastcup.api.infrastructure.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Component
public class AppleClient {

    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final RestClient restClient;
    private final String tokenUrl;
    private final String keyUrl;
    private final String clientId;
    private final String teamId;
    private final String keyId;
    private final String privateKey;
    private final String redirectUri;

    public AppleClient(
            @Value("${app.oauth.apple.token-url:https://appleid.apple.com/auth/token}") String tokenUrl,
            @Value("${app.oauth.apple.key-url:https://appleid.apple.com/auth/keys}") String keyUrl,
            @Value("${app.oauth.apple.client-id:}") String clientId,
            @Value("${app.oauth.apple.team-id:}") String teamId,
            @Value("${app.oauth.apple.key-id:}") String keyId,
            @Value("${app.oauth.apple.private-key:}") String privateKey,
            @Value("${app.oauth.apple.redirect-uri:}") String redirectUri
    ) {
        this.restClient = RestClient.builder().build();
        this.tokenUrl = tokenUrl;
        this.keyUrl = keyUrl;
        this.clientId = clientId;
        this.teamId = teamId;
        this.keyId = keyId;
        this.privateKey = privateKey;
        this.redirectUri = redirectUri;
    }

    public VerifiedOAuthUser verifyAuthorizationCode(String authorizationCode, String identityToken) {
        validateAuthorizationCode(authorizationCode);
        validateClientConfig();

        AppleTokenResponse tokenResponse = exchangeToken(authorizationCode);
        AppleIdTokenClaims claims = verifyIdToken(tokenResponse.idToken);

        if (identityToken != null && !identityToken.isBlank()) {
            AppleIdTokenClaims clientClaims = verifyIdToken(identityToken);
            if (!claims.subject.equals(clientClaims.subject)) {
                throw new OAuthVerificationException("APPLE_ID_TOKEN_SUB_MISMATCH");
            }
            if (claims.email == null || claims.email.isBlank()) {
                claims = claims.withEmail(clientClaims.email);
            }
        }

        return new VerifiedOAuthUser(claims.subject, claims.email, null);
    }

    private void validateAuthorizationCode(String code) {
        if (code == null || code.isBlank()) {
            throw new OAuthVerificationException("APPLE_AUTHORIZATION_CODE_EMPTY");
        }
    }

    private void validateClientConfig() {
        if (clientId == null || clientId.isBlank()) {
            throw new OAuthVerificationException("APPLE_CLIENT_ID_EMPTY");
        }
        if (teamId == null || teamId.isBlank()) {
            throw new OAuthVerificationException("APPLE_TEAM_ID_EMPTY");
        }
        if (keyId == null || keyId.isBlank()) {
            throw new OAuthVerificationException("APPLE_KEY_ID_EMPTY");
        }
        if (privateKey == null || privateKey.isBlank()) {
            throw new OAuthVerificationException("APPLE_PRIVATE_KEY_EMPTY");
        }
    }

    private AppleTokenResponse exchangeToken(String code) {
        try {
            AppleTokenResponse response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(createTokenRequest(code))
                    .retrieve()
                    .body(AppleTokenResponse.class);

            if (response != null && response.idToken != null && !response.idToken.isBlank()) {
                return response;
            }
        } catch (RestClientException ignored) {
        }
        throw new OAuthVerificationException("APPLE_TOKEN_EXCHANGE_FAILED");
    }

    private MultiValueMap<String, String> createTokenRequest(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", createClientSecret());
        if (redirectUri != null && !redirectUri.isBlank()) {
            form.add("redirect_uri", redirectUri);
        }
        return form;
    }

    private String createClientSecret() {
        PrivateKey key = parsePrivateKey(privateKey);
        Instant now = Instant.now();
        return Jwts.builder()
                .header().add("kid", keyId).and()
                .issuer(teamId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(5, ChronoUnit.MINUTES)))
                .audience().add(APPLE_ISSUER).and()
                .subject(clientId)
                .signWith(key, Jwts.SIG.ES256)
                .compact();
    }

    private PrivateKey parsePrivateKey(String key) {
        try {
            String normalized = key
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(normalized);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            return KeyFactory.getInstance("EC").generatePrivate(spec);
        } catch (Exception ex) {
            throw new OAuthVerificationException("APPLE_PRIVATE_KEY_INVALID");
        }
    }

    private AppleIdTokenClaims verifyIdToken(String idToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(idToken);
            JWSHeader header = signedJWT.getHeader();
            RSAKey rsaKey = findAppleKey(header.getKeyID());
            RSAPublicKey publicKey = rsaKey.toRSAPublicKey();
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                throw new OAuthVerificationException("APPLE_ID_TOKEN_INVALID");
            }
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            validateClaims(claims);
            return new AppleIdTokenClaims(
                    claims.getSubject(),
                    claims.getStringClaim("email")
            );
        } catch (OAuthVerificationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OAuthVerificationException("APPLE_ID_TOKEN_INVALID");
        }
    }

    private void validateClaims(JWTClaimsSet claims) {
        if (claims == null) {
            throw new OAuthVerificationException("APPLE_ID_TOKEN_INVALID");
        }
        if (!APPLE_ISSUER.equals(claims.getIssuer())) {
            throw new OAuthVerificationException("APPLE_ID_TOKEN_ISSUER_INVALID");
        }
        List<String> audiences = claims.getAudience();
        if (audiences == null || !audiences.contains(clientId)) {
            throw new OAuthVerificationException("APPLE_ID_TOKEN_AUDIENCE_INVALID");
        }
        Date expiration = claims.getExpirationTime();
        if (expiration == null || expiration.before(new Date())) {
            throw new OAuthVerificationException("APPLE_ID_TOKEN_EXPIRED");
        }
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new OAuthVerificationException("APPLE_SUB_EMPTY");
        }
    }

    private RSAKey findAppleKey(String keyId) {
        try {
            String jwkJson = restClient.get()
                    .uri(keyUrl)
                    .retrieve()
                    .body(String.class);
            if (jwkJson == null || jwkJson.isBlank()) {
                throw new OAuthVerificationException("APPLE_JWK_FETCH_FAILED");
            }
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
        } catch (Exception ex) {
            throw new OAuthVerificationException("APPLE_JWK_FETCH_FAILED");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AppleTokenResponse {
        @JsonProperty("id_token")
        public String idToken;
    }

    private record AppleIdTokenClaims(String subject, String email) {
        private AppleIdTokenClaims withEmail(String newEmail) {
            return new AppleIdTokenClaims(this.subject, newEmail);
        }
    }
}
