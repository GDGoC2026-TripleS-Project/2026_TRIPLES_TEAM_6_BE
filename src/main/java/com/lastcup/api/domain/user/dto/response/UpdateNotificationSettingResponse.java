package com.lastcup.api.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateNotificationSettingResponse(
        @Schema(description = "수정 성공 여부", example = "true")
        boolean updated
) {
}
