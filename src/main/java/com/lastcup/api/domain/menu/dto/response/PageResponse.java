package com.lastcup.api.domain.menu.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "페이지네이션 응답")
public record PageResponse<T>(
        @Schema(description = "현재 페이지의 아이템")
        List<T> content,

        @Schema(description = "현재 페이지 번호(0부터)", example = "0")
        int page,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
}
