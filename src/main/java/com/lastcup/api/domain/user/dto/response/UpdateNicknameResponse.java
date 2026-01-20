package com.lastcup.api.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateNicknameResponse(
        @Schema(description = "유저 ID", example = "1")
        Long id,

        @Schema(description = "변경된 닉네임", example = "새닉네임")
        String nickname
) {
}
