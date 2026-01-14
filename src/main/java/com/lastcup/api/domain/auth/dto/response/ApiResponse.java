package com.lastcup.api.domain.auth.dto.response;

public record ApiResponse<T>(T data) {
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }
}
