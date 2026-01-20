package com.lastcup.api.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(description = "로그인 아이디", example = "user123")
        @NotBlank
        @Size(min = 4, max = 100)
        String loginId,

        @Schema(description = "비밀번호", example = "password123!")
        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {
}
