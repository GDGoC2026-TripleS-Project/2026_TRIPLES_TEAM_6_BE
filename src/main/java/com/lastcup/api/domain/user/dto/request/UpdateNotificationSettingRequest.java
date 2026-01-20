package com.lastcup.api.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

public record UpdateNotificationSettingRequest(
        @Schema(description = "전체 알림 on/off", example = "true")
        Boolean isEnabled,

        @Schema(description = "기록 유도 알림 시각(HH:mm:ss)", example = "15:00:00")
        LocalTime recordRemindAt,

        @Schema(description = "하루 마무리 알림 시각(HH:mm:ss)", example = "22:00:00")
        LocalTime dailyCloseAt
) {
}
