package com.lastcup.api.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProfileImageResponse(
        @Schema(description = "프로필 이미지 URL", example = "https://.../profile.jpg")
        String profileImageUrl
) {
}
