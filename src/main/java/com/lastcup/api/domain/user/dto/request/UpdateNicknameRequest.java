package com.lastcup.api.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
        @Schema(description = "변경할 닉네임", example = "새닉네임")
        @NotBlank
        @Size(min = 1, max = 50)
        String nickname
) {
}
