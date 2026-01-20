package com.lastcup.api.global.error;

import org.springframework.http.HttpStatus;

public enum StorageErrorCode implements ErrorCode {
    STORAGE_INVALID_CONTENT_TYPE("STORAGE_INVALID_CONTENT_TYPE", "허용되지 않은 파일 형식입니다.", HttpStatus.BAD_REQUEST),
    STORAGE_UPLOAD_FAILED("STORAGE_UPLOAD_FAILED", "파일 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    StorageErrorCode(String code, String message, HttpStatus httpStatus) {
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
