package com.lastcup.api.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record RegisterDeviceResponse(
        @Schema(description = "성공 여부", example = "true")
        boolean success
) {
}
