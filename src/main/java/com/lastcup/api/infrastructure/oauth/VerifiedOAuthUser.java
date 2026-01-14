package com.lastcup.api.infrastructure.oauth;

public record VerifiedOAuthUser(
        String providerUserKey,
        String email,
        String profileImageUrl
) {
}
