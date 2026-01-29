package com.lastcup.api.domain.user.controller;

import com.lastcup.api.global.response.ApiResponse;
import com.lastcup.api.domain.user.dto.request.UpdateNotificationSettingRequest;
import com.lastcup.api.domain.user.dto.response.NotificationSettingResponse;
import com.lastcup.api.domain.user.dto.response.UpdateNotificationSettingResponse;
import com.lastcup.api.domain.user.service.UserNotificationSettingService;
import com.lastcup.api.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "유저(알림 설정) API")
@RestController
@RequestMapping("/api/v1/users/me/notification-settings")
public class UserNotificationSettingController {

    private final UserNotificationSettingService settingService;

    public UserNotificationSettingController(UserNotificationSettingService settingService) {
        this.settingService = settingService;
    }

    @Operation(summary = "알림 설정 조회(없으면 자동 생성)", description = "설정이 없으면 기본값으로 생성 후 반환합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping
    public ApiResponse<NotificationSettingResponse> findSettings(@AuthenticationPrincipal AuthUser authUser) {
        NotificationSettingResponse response = settingService.findOrCreate(authUser.userId());
        return ApiResponse.success(response);
    }

    @Operation(summary = "알림 설정 변경", description = "부분 수정이 가능하며, 설정이 없으면 기본값 생성 후 수정합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PatchMapping
    public ApiResponse<UpdateNotificationSettingResponse> updateSettings(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody UpdateNotificationSettingRequest request
    ) {
        UpdateNotificationSettingResponse response = settingService.update(
                authUser.userId(),
                request.isEnabled(),
                request.recordRemindAt(),
                request.dailyCloseAt()
        );
        return ApiResponse.success(response);
    }
}
