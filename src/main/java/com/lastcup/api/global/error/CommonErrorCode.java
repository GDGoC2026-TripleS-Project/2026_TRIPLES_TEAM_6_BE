package com.lastcup.api.global.error;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {
    COMMON_VALIDATION_FAILED("COMMON_VALIDATION_FAILED", "요청 값이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    COMMON_INVALID_JSON("COMMON_INVALID_JSON", "요청 본문을 해석할 수 없습니다.", HttpStatus.BAD_REQUEST),
    COMMON_TYPE_MISMATCH("COMMON_TYPE_MISMATCH", "요청 값의 타입이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    COMMON_MISSING_PARAM("COMMON_MISSING_PARAM", "필수 요청 파라미터가 누락되었습니다.", HttpStatus.BAD_REQUEST),
    COMMON_BAD_REQUEST("COMMON_BAD_REQUEST", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    COMMON_CONFLICT("COMMON_CONFLICT", "요청이 현재 상태와 충돌합니다.", HttpStatus.CONFLICT),
    COMMON_NOT_FOUND("COMMON_NOT_FOUND", "대상을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    COMMON_INTERNAL_ERROR("COMMON_INTERNAL_ERROR", "서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    CommonErrorCode(String code, String message, HttpStatus httpStatus) {
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
