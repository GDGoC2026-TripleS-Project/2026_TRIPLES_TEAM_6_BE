package com.lastcup.api.infrastructure.oauth;

public class OAuthVerificationException extends RuntimeException {

    public OAuthVerificationException(String message) {
        super(message);
    }
}
