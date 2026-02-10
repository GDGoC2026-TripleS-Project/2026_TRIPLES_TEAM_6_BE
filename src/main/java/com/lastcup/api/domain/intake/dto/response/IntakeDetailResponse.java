package com.lastcup.api.domain.intake.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "섭취 기록 상세 응답")
public record IntakeDetailResponse(

        @Schema(description = "섭취 기록 ID", example = "1")
        Long id,

        @Schema(description = "섭취 날짜", example = "2026-01-26")
        LocalDate intakeDate,

        // ── 수정 플로우에서 프론트엔드가 기존 선택 화면을 복원하는 데 필요한 ID ──
        @Schema(description = "브랜드 ID (음료 리스트·옵션 목록 조회용)", example = "1")
        Long brandId,

        @Schema(description = "메뉴 ID (메뉴 상세·사이즈 목록 조회용)", example = "10")
        Long menuId,

        @Schema(description = "메뉴 사이즈 ID (수정 요청 시 전송용)", example = "42")
        Long menuSizeId,

        @Schema(description = "브랜드명", example = "이디야")
        String brandName,

        @Schema(description = "메뉴명", example = "에스프레소 쉐이크")
        String menuName,

        @Schema(description = "온도", example = "ICED")
        String temperature,

        @Schema(description = "사이즈명", example = "Large")
        String sizeName,

        @Schema(description = "수량", example = "1")
        int quantity,

        @Schema(description = "카페인(mg)", example = "158")
        int caffeineSnapshot,

        @Schema(description = "당류(g)", example = "19")
        int sugarSnapshot,

        @Schema(description = "에스프레소 환산 잔 수 (75mg = 1잔)", example = "2")
        int espressoShotCount,

        @Schema(description = "각설탕 환산 개수 (3g = 1개)", example = "6")
        int sugarCubeCount,

        @Schema(description = "칼로리(kcal)")
        Integer caloriesSnapshot,

        @Schema(description = "나트륨(mg)")
        Integer sodiumSnapshot,

        @Schema(description = "단백질(g)")
        Integer proteinSnapshot,

        @Schema(description = "지방(g)")
        Integer fatSnapshot,

        @Schema(description = "목표 카페인(mg)", example = "400")
        int goalCaffeineTargetSnapshot,

        @Schema(description = "목표 당류(g)", example = "25")
        int goalSugarTargetSnapshot,

        @Schema(description = "추가 옵션 목록")
        List<IntakeOptionDetailResponse> options,

        @Schema(description = "기록 시각")
        LocalDateTime createdAt
) {
}
