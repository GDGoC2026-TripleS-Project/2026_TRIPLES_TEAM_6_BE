package com.lastcup.api.security;

import com.lastcup.api.global.error.ErrorCode;

public class JwtValidationException extends RuntimeException {

    private final ErrorCode errorCode;

    public JwtValidationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
