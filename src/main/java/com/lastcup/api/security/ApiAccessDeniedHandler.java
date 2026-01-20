package com.lastcup.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lastcup.api.global.error.AuthErrorCode;
import com.lastcup.api.global.response.ApiError;
import com.lastcup.api.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ApiAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        ApiError apiError = new ApiError(
                AuthErrorCode.AUTH_FORBIDDEN.getCode(),
                AuthErrorCode.AUTH_FORBIDDEN.getMessage(),
                null
        );
        ApiResponse<Void> body = ApiResponse.failure(apiError);
        response.setStatus(AuthErrorCode.AUTH_FORBIDDEN.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
