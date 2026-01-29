package com.lastcup.api.domain.user.controller;

import com.lastcup.api.global.response.ApiResponse;
import com.lastcup.api.domain.user.dto.request.UpdateNicknameRequest;
import com.lastcup.api.domain.user.dto.response.DeleteUserResponse;
import com.lastcup.api.domain.user.dto.response.ProfileImageResponse;
import com.lastcup.api.domain.user.dto.response.UpdateNicknameResponse;
import com.lastcup.api.domain.user.dto.response.UserMeResponse;
import com.lastcup.api.domain.user.service.UserService;
import com.lastcup.api.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User", description = "유저(내 정보) API")
@RestController
@RequestMapping("/api/v1/users/me")
public class UserMeController {

    private final UserService userService;

    public UserMeController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "내 정보 조회", description = "로그인된 유저의 정보를 조회합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping
    public ApiResponse<UserMeResponse> findMe(@AuthenticationPrincipal AuthUser authUser) {
        UserMeResponse response = userService.findMe(authUser.userId());
        return ApiResponse.success(response);
    }

    @Operation(summary = "내 닉네임 수정", description = "닉네임을 변경합니다(중복 닉네임 불가).")
    @SecurityRequirement(name = "BearerAuth")
    @PatchMapping
    public ApiResponse<UpdateNicknameResponse> updateNickname(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody @Valid UpdateNicknameRequest request
    ) {
        UpdateNicknameResponse response = userService.updateNickname(authUser.userId(), request.nickname());
        return ApiResponse.success(response);
    }

    @Operation(summary = "프로필 이미지 업로드", description = "multipart/form-data로 이미지를 업로드하고 profileImageUrl을 갱신합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PatchMapping(value = "/profile-image", consumes = "multipart/form-data")
    public ApiResponse<ProfileImageResponse> updateProfileImage(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestPart("file") MultipartFile file
    ) {
        ProfileImageResponse response = userService.updateProfileImage(authUser.userId(), file);
        return ApiResponse.success(response);
    }

    @Operation(summary = "회원 탈퇴", description = "회원 상태를 DELETED로 변경합니다(soft delete).")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping
    public ApiResponse<DeleteUserResponse> deleteMe(@AuthenticationPrincipal AuthUser authUser) {
        DeleteUserResponse response = userService.deleteMe(authUser.userId());
        return ApiResponse.success(response);
    }
}
