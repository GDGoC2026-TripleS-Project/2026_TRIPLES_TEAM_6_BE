package com.lastcup.api.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record NotificationSettingResponse(
        @Schema(description = "전체 알림 on/off", example = "true")
        boolean isEnabled,

        @Schema(description = "기록 유도 알림 시각(HH:mm:ss)", example = "14:00:00")
        LocalTime recordRemindAt,

        @Schema(description = "하루 마무리 알림 시각(HH:mm:ss)", example = "21:00:00")
        LocalTime dailyCloseAt,

        @Schema(description = "생성일", example = "2026-01-14T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정일", example = "2026-01-14T10:00:00")
        LocalDateTime updatedAt
) {
}
