package com.lastcup.api.security;

public class JwtValidationException extends RuntimeException {

    public JwtValidationException(String message) {
        super(message);
    }
}
