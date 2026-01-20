package com.lastcup.api.global.error;

import org.springframework.http.HttpStatus;

public enum JwtErrorCode implements ErrorCode {
    JWT_TOKEN_MISSING("JWT_TOKEN_MISSING", "인증 토큰이 필요합니다.", HttpStatus.UNAUTHORIZED),
    JWT_ACCESS_INVALID("JWT_ACCESS_INVALID", "액세스 토큰이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),
    JWT_REFRESH_INVALID("JWT_REFRESH_INVALID", "리프레시 토큰이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),
    JWT_TOKEN_TYPE_MISMATCH("JWT_TOKEN_TYPE_MISMATCH", "토큰 타입이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    JwtErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
