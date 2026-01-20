package com.lastcup.api.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AvailabilityResponse(
        @Schema(description = "사용 가능 여부", example = "true")
        boolean isAvailable
) {
}
