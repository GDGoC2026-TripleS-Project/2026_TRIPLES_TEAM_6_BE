package com.lastcup.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lastcup.api.global.error.ErrorCode;
import com.lastcup.api.global.error.JwtErrorCode;
import com.lastcup.api.global.response.ApiError;
import com.lastcup.api.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ErrorCode errorCode = resolveErrorCode(request, authException);
        ApiError apiError = new ApiError(errorCode.getCode(), errorCode.getMessage(), null);
        ApiResponse<Void> body = ApiResponse.failure(apiError);
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private ErrorCode resolveErrorCode(HttpServletRequest request, AuthenticationException exception) {
        Object attribute = request.getAttribute("authErrorCode");
        if (attribute instanceof ErrorCode errorCode) {
            return errorCode;
        }
        if (exception instanceof JwtAuthenticationException jwtException) {
            return jwtException.getErrorCode();
        }
        return JwtErrorCode.JWT_TOKEN_MISSING;
    }
}
