package com.lastcup.api.global.error;

import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {
    AUTH_DUPLICATE_LOGIN_ID("AUTH_DUPLICATE_LOGIN_ID", "이미 사용 중인 아이디입니다.", HttpStatus.CONFLICT),
    AUTH_DUPLICATE_NICKNAME("AUTH_DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    AUTH_PROVIDER_INVALID("AUTH_PROVIDER_INVALID", "지원하지 않는 소셜 제공자입니다.", HttpStatus.BAD_REQUEST),
    AUTH_AUTHORIZATION_CODE_REQUIRED("AUTH_AUTHORIZATION_CODE_REQUIRED", "인가 코드가 필요합니다.", HttpStatus.BAD_REQUEST),
    AUTH_FORBIDDEN("AUTH_FORBIDDEN", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    AuthErrorCode(String code, String message, HttpStatus httpStatus) {
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
