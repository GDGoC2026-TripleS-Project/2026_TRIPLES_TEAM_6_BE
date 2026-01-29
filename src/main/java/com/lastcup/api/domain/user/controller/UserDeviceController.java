package com.lastcup.api.domain.user.controller;

import com.lastcup.api.global.response.ApiResponse;
import com.lastcup.api.domain.user.dto.request.RegisterDeviceRequest;
import com.lastcup.api.domain.user.dto.response.RegisterDeviceResponse;
import com.lastcup.api.domain.user.service.UserDeviceService;
import com.lastcup.api.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "유저(기기) API")
@RestController
@RequestMapping("/api/v1/users/me/devices")
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    public UserDeviceController(UserDeviceService userDeviceService) {
        this.userDeviceService = userDeviceService;
    }

    @Operation(summary = "기기 토큰(FCM) 등록/갱신", description = "UNIQUE(fcmToken) 기반으로 upsert하며 lastSeenAt을 갱신합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping
    public ApiResponse<RegisterDeviceResponse> registerDevice(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody @Valid RegisterDeviceRequest request
    ) {
        RegisterDeviceResponse response = userDeviceService.createOrUpdateDevice(
                authUser.userId(),
                request.fcmToken(),
                request.platform()
        );
        return ApiResponse.success(response);
    }
}
