package com.lastcup.api.domain.intake.controller;

import com.lastcup.api.domain.intake.dto.request.CreateIntakeRequest;
import com.lastcup.api.domain.intake.dto.request.IntakeUpdateRequest;
import com.lastcup.api.domain.intake.dto.response.DailyIntakeSummaryResponse;
import com.lastcup.api.domain.intake.dto.response.IntakeDetailResponse;
import com.lastcup.api.domain.intake.dto.response.IntakeResponse;
import com.lastcup.api.domain.intake.dto.response.PeriodIntakeSummaryResponse;
import com.lastcup.api.domain.intake.service.IntakeService;
import com.lastcup.api.global.response.ApiResponse;
import com.lastcup.api.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Intake", description = "섭취 기록 API")
@RestController
@RequestMapping("/api/v1/intakes")
public class IntakeController {

    private final IntakeService intakeService;

    public IntakeController(IntakeService intakeService) {
        this.intakeService = intakeService;
    }

    @Operation(summary = "섭취 기록 생성", description = "음료 섭취를 기록합니다. 영양/목표 스냅샷이 자동 저장됩니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IntakeResponse> createIntake(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody @Valid CreateIntakeRequest request
    ) {
        IntakeResponse response = intakeService.createIntake(authUser.userId(), request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "일별 섭취 이력 조회", description = "특정 날짜의 섭취 이력과 요약을 조회합니다. 날짜 미지정 시 오늘 기준으로 조회합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/daily")
    public ApiResponse<DailyIntakeSummaryResponse> findDailyIntakes(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "조회 날짜 (미입력 시 오늘)", example = "2026-01-26")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        DailyIntakeSummaryResponse response = intakeService.findDailyIntakes(authUser.userId(), date);
        return ApiResponse.success(response);
    }

    @Operation(summary = "기간별 섭취 이력 조회", description = "시작~종료 날짜 범위의 섭취 이력과 요약을 조회합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/period")
    public ApiResponse<PeriodIntakeSummaryResponse> findPeriodIntakes(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "시작 날짜", example = "2026-02-07")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료 날짜", example = "2026-02-22")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        PeriodIntakeSummaryResponse response = intakeService.findPeriodIntakes(authUser.userId(), startDate, endDate);
        return ApiResponse.success(response);
    }

    @Operation(summary = "섭취 기록 상세 조회", description = "특정 섭취 기록의 전체 영양성분 상세를 조회합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/{intakeId}")
    public ApiResponse<IntakeDetailResponse> findIntakeDetail(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long intakeId
    ) {
        IntakeDetailResponse response = intakeService.findIntakeDetail(authUser.userId(), intakeId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "섭취 기록 수정", description = "특정 섭취 기록의 음료·날짜·수량·옵션을 수정합니다. 영양 스냅샷이 재계산됩니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PutMapping("/{intakeId}")
    public ApiResponse<IntakeResponse> updateIntake(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long intakeId,
            @RequestBody @Valid IntakeUpdateRequest request
    ) {
        IntakeResponse response = intakeService.updateIntake(authUser.userId(), intakeId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "섭취 기록 삭제", description = "특정 섭취 기록을 삭제합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{intakeId}")
    public ApiResponse<Void> deleteIntake(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long intakeId
    ) {
        intakeService.deleteIntake(authUser.userId(), intakeId);
        return ApiResponse.success(null);
    }
}
