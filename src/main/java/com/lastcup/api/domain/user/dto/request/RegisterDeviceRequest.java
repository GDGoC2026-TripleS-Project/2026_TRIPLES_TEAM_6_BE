package com.lastcup.api.domain.user.dto.request;

import com.lastcup.api.domain.user.domain.UserPlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(
        @Schema(description = "FCM 토큰", example = "bk3RNwTe3H0:CI2k_HHwgIwd...")
        @NotBlank
        @Size(min = 1, max = 500)
        String fcmToken,

        @Schema(description = "플랫폼", example = "ANDROID")
        UserPlatform platform
) {
}
