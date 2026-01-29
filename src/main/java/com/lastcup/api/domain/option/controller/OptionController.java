package com.lastcup.api.domain.option.controller;

import com.lastcup.api.global.response.ApiResponse;
import com.lastcup.api.domain.option.domain.Option;
import com.lastcup.api.domain.option.domain.OptionCategory;
import com.lastcup.api.domain.option.dto.response.OptionResponse;
import com.lastcup.api.domain.option.service.OptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Option", description = "브랜드 옵션 조회 API")
@RestController
@RequestMapping("/api/v1/brands/{brandId}/options")
public class OptionController {

    private final OptionService optionService;

    public OptionController(OptionService optionService) {
        this.optionService = optionService;
    }

    @Operation(
            summary = "브랜드 옵션 목록 조회",
            description = "해당 브랜드(카페)가 제공하는 고정 옵션 목록을 조회합니다. category는 선택입니다."
    )
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping
    public ApiResponse<List<OptionResponse>> findBrandOptions(
            @PathVariable Long brandId,
            @RequestParam(required = false) OptionCategory category
    ) {
        List<Option> options = optionService.findBrandOptions(brandId, category);
        List<OptionResponse> response = options.stream()
                .map(OptionResponse::from)
                .toList();

        return ApiResponse.success(response);
    }
}
