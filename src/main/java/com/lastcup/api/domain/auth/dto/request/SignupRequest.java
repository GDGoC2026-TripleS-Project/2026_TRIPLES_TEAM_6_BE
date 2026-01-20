package com.lastcup.api.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Schema(description = "로그인 아이디", example = "user123")
        @NotBlank
        @Size(min = 4, max = 100)
        String loginId,

        @Schema(description = "비밀번호", example = "password123!")
        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @Schema(description = "닉네임", example = "커피러버")
        @NotBlank
        @Size(min = 1, max = 50)
        String nickname
) {
}
