package com.lastcup.api.global.error;

import org.springframework.http.HttpStatus;

public enum OAuthErrorCode implements ErrorCode {
    OAUTH_GOOGLE_TOKEN_INVALID("OAUTH_GOOGLE_TOKEN_INVALID", "구글 토큰이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),
    OAUTH_KAKAO_TOKEN_EXCHANGE_FAILED("OAUTH_KAKAO_TOKEN_EXCHANGE_FAILED", "카카오 토큰 발급에 실패했습니다.", HttpStatus.BAD_REQUEST),
    OAUTH_KAKAO_USER_INFO_FETCH_FAILED("OAUTH_KAKAO_USER_INFO_FETCH_FAILED", "카카오 사용자 정보 조회에 실패했습니다.", HttpStatus.BAD_REQUEST),
    OAUTH_KAKAO_ID_MISSING("OAUTH_KAKAO_ID_MISSING", "카카오 사용자 식별 정보가 누락되었습니다.", HttpStatus.UNAUTHORIZED),
    OAUTH_KAKAO_AUTHORIZATION_CODE_EMPTY("OAUTH_KAKAO_AUTHORIZATION_CODE_EMPTY", "카카오 인가 코드가 필요합니다.", HttpStatus.BAD_REQUEST),
    OAUTH_KAKAO_PROVIDER_USER_KEY_EMPTY("OAUTH_KAKAO_PROVIDER_USER_KEY_EMPTY", "카카오 사용자 키가 누락되었습니다.", HttpStatus.BAD_REQUEST),
    OAUTH_KAKAO_CLIENT_ID_EMPTY("OAUTH_KAKAO_CLIENT_ID_EMPTY", "카카오 클라이언트 아이디가 누락되었습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    OAuthErrorCode(String code, String message, HttpStatus httpStatus) {
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
