package com.lastcup.api.infrastructure.oauth;

public interface OAuthTokenVerifier {
    SocialProvider getProvider();

    VerifiedOAuthUser verify(String providerAccessToken);
}
