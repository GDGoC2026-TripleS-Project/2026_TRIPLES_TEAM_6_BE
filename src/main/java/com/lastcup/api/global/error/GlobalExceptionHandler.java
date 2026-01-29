package com.lastcup.api.global.error;

import com.lastcup.api.global.response.ApiError;
import com.lastcup.api.global.response.ApiResponse;
import com.lastcup.api.infrastructure.oauth.OAuthVerificationException;
import com.lastcup.api.security.JwtValidationException;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();
        return buildResponse(CommonErrorCode.COMMON_VALIDATION_FAILED, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidJson(HttpMessageNotReadableException ex) {
        return buildResponse(CommonErrorCode.COMMON_INVALID_JSON);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildResponse(CommonErrorCode.COMMON_TYPE_MISMATCH);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        return buildResponse(CommonErrorCode.COMMON_MISSING_PARAM);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorCode errorCode = resolveIllegalArgumentCode(ex.getMessage());
        return buildResponse(errorCode);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        ErrorCode errorCode = resolveIllegalStateCode(ex.getMessage());
        return buildResponse(errorCode);
    }

    @ExceptionHandler(OAuthVerificationException.class)
    public ResponseEntity<ApiResponse<Void>> handleOAuth(OAuthVerificationException ex) {
        ErrorCode errorCode = resolveOAuthCode(ex.getMessage());
        return buildResponse(errorCode);
    }

    @ExceptionHandler(JwtValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwt(JwtValidationException ex) {
        return buildResponse(ex.getErrorCode());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(DataIntegrityViolationException ex) {
        return buildResponse(CommonErrorCode.COMMON_CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        return buildResponse(CommonErrorCode.COMMON_INTERNAL_ERROR);
    }

    private ApiError.FieldError toFieldError(FieldError error) {
        return new ApiError.FieldError(
                error.getField(),
                Objects.requireNonNullElse(error.getDefaultMessage(), "invalid"),
                error.getRejectedValue()
        );
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(ErrorCode errorCode) {
        return buildResponse(errorCode, null);
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(ErrorCode errorCode, List<ApiError.FieldError> fieldErrors) {
        ApiError apiError = new ApiError(errorCode.getCode(), errorCode.getMessage(), fieldErrors);
        ApiResponse<Void> response = ApiResponse.failure(apiError);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    private ErrorCode resolveIllegalArgumentCode(String message) {
        if (message == null) {
            return CommonErrorCode.COMMON_BAD_REQUEST;
        }
        return switch (message) {
            case "loginId already exists" -> AuthErrorCode.AUTH_DUPLICATE_LOGIN_ID;
            case "nickname already exists" -> AuthErrorCode.AUTH_DUPLICATE_NICKNAME;
            case "loginId not found", "password mismatch", "user is not active" ->
                    AuthErrorCode.AUTH_INVALID_CREDENTIALS;
            case "unsupported provider" -> AuthErrorCode.AUTH_PROVIDER_INVALID;
            case "authorizationCode is required" -> AuthErrorCode.AUTH_AUTHORIZATION_CODE_REQUIRED;
            case "Bearer Token is missing" -> JwtErrorCode.JWT_TOKEN_MISSING;
            case "user not found" -> UserErrorCode.USER_NOT_FOUND;
            case "file is empty" -> UserErrorCode.USER_PROFILE_IMAGE_REQUIRED;
            case "file is not image" -> StorageErrorCode.STORAGE_INVALID_CONTENT_TYPE;
            default -> resolveCommonBadRequest(message);
        };
    }

    private ErrorCode resolveCommonBadRequest(String message) {
        if (message.startsWith("Menu not found")
                || message.startsWith("Brand not found")
                || message.startsWith("MenuTemperature not found")
                || message.startsWith("MenuSize not found")) {
            return CommonErrorCode.COMMON_NOT_FOUND;
        }
        return CommonErrorCode.COMMON_BAD_REQUEST;
    }

    private ErrorCode resolveIllegalStateCode(String message) {
        if ("user not found".equals(message)) {
            return UserErrorCode.USER_NOT_FOUND;
        }
        if ("s3 upload failed".equals(message)) {
            return StorageErrorCode.STORAGE_UPLOAD_FAILED;
        }
        return CommonErrorCode.COMMON_INTERNAL_ERROR;
    }

    private ErrorCode resolveOAuthCode(String message) {
        if (message == null) {
            return CommonErrorCode.COMMON_INTERNAL_ERROR;
        }
        return switch (message) {
            case "GOOGLE_ID_TOKEN_INVALID" -> OAuthErrorCode.OAUTH_GOOGLE_TOKEN_INVALID;
            case "KAKAO_TOKEN_EXCHANGE_FAILED" -> OAuthErrorCode.OAUTH_KAKAO_TOKEN_EXCHANGE_FAILED;
            case "KAKAO_USER_INFO_FETCH_FAILED" -> OAuthErrorCode.OAUTH_KAKAO_USER_INFO_FETCH_FAILED;
            case "KAKAO_ID_MISSING" -> OAuthErrorCode.OAUTH_KAKAO_ID_MISSING;
            case "KAKAO_AUTHORIZATION_CODE_EMPTY" -> OAuthErrorCode.OAUTH_KAKAO_AUTHORIZATION_CODE_EMPTY;
            case "KAKAO_PROVIDER_USER_KEY_EMPTY" -> OAuthErrorCode.OAUTH_KAKAO_PROVIDER_USER_KEY_EMPTY;
            case "KAKAO_CLIENT_ID_EMPTY" -> OAuthErrorCode.OAUTH_KAKAO_CLIENT_ID_EMPTY;
            case "APPLE_TOKEN_EXCHANGE_FAILED" -> OAuthErrorCode.OAUTH_APPLE_TOKEN_EXCHANGE_FAILED;
            case "APPLE_AUTHORIZATION_CODE_EMPTY" -> OAuthErrorCode.OAUTH_APPLE_AUTHORIZATION_CODE_EMPTY;
            case "APPLE_CLIENT_ID_EMPTY" -> OAuthErrorCode.OAUTH_APPLE_CLIENT_ID_EMPTY;
            case "APPLE_TEAM_ID_EMPTY" -> OAuthErrorCode.OAUTH_APPLE_TEAM_ID_EMPTY;
            case "APPLE_KEY_ID_EMPTY" -> OAuthErrorCode.OAUTH_APPLE_KEY_ID_EMPTY;
            case "APPLE_PRIVATE_KEY_EMPTY" -> OAuthErrorCode.OAUTH_APPLE_PRIVATE_KEY_EMPTY;
            case "APPLE_PRIVATE_KEY_INVALID" -> OAuthErrorCode.OAUTH_APPLE_PRIVATE_KEY_INVALID;
            case "APPLE_ID_TOKEN_INVALID" -> OAuthErrorCode.OAUTH_APPLE_ID_TOKEN_INVALID;
            case "APPLE_ID_TOKEN_ISSUER_INVALID" -> OAuthErrorCode.OAUTH_APPLE_ID_TOKEN_ISSUER_INVALID;
            case "APPLE_ID_TOKEN_AUDIENCE_INVALID" -> OAuthErrorCode.OAUTH_APPLE_ID_TOKEN_AUDIENCE_INVALID;
            case "APPLE_ID_TOKEN_EXPIRED" -> OAuthErrorCode.OAUTH_APPLE_ID_TOKEN_EXPIRED;
            case "APPLE_SUB_EMPTY" -> OAuthErrorCode.OAUTH_APPLE_SUB_EMPTY;
            case "APPLE_JWK_FETCH_FAILED" -> OAuthErrorCode.OAUTH_APPLE_JWK_FETCH_FAILED;
            case "APPLE_JWK_NOT_FOUND" -> OAuthErrorCode.OAUTH_APPLE_JWK_NOT_FOUND;
            case "APPLE_JWK_TYPE_INVALID" -> OAuthErrorCode.OAUTH_APPLE_JWK_TYPE_INVALID;
            case "APPLE_ID_TOKEN_SUB_MISMATCH" -> OAuthErrorCode.OAUTH_APPLE_ID_TOKEN_SUB_MISMATCH;
            default -> CommonErrorCode.COMMON_INTERNAL_ERROR;
        };
    }
}
