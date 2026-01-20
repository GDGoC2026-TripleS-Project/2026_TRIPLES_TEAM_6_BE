package com.lastcup.api.domain.user.dto.response;

import com.lastcup.api.domain.user.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record DeleteUserResponse(
        @Schema(description = "변경된 상태", example = "DELETED")
        UserStatus status
) {
}
