package com.lastcup.api.domain.option.service;

import com.lastcup.api.domain.option.domain.Option;
import com.lastcup.api.domain.option.domain.OptionCategory;
import com.lastcup.api.domain.option.domain.OptionSelectionType;
import com.lastcup.api.domain.option.repository.OptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * OptionService 단위 테스트.
 * 브랜드별 옵션 조회, 카테고리 필터링, brandId 유효성 검증을 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class OptionServiceTest {

    @Mock private OptionRepository optionRepository;

    @InjectMocks
    private OptionService optionService;

    // ── 테스트 픽스처 헬퍼 ──

    private Option createOption(Long brandId, String name, OptionCategory category) {
        return Option.create(brandId, name, category, OptionSelectionType.COUNT);
    }

    // 1. findBrandOptions — 브랜드 옵션 조회
    // category null → 전체 조회 / category 있음 → 카테고리 필터

    @Nested
    @DisplayName("브랜드 옵션 조회 (findBrandOptions)")
    class FindBrandOptions {

        @Test
        @DisplayName("카테고리 null — 전체 옵션 조회")
        void findAllOptions() {
            // given — category가 null이면 findAllWithNutritionByBrandId 호출
            Option syrup = createOption(1L, "바닐라 시럽", OptionCategory.SYRUP);
            Option shot = createOption(1L, "에스프레소 샷", OptionCategory.SHOT);
            when(optionRepository.findAllWithNutritionByBrandId(1L))
                    .thenReturn(List.of(syrup, shot));

            // when
            List<Option> result = optionService.findBrandOptions(1L, null);

            // then
            assertEquals(2, result.size());
            verify(optionRepository).findAllWithNutritionByBrandId(1L);
            verify(optionRepository, never()).findAllWithNutritionByBrandIdAndCategory(any(), any());
        }

        @Test
        @DisplayName("카테고리 지정 — 해당 카테고리만 조회")
        void findOptionsByCategory() {
            // given — category가 있으면 findAllWithNutritionByBrandIdAndCategory 호출
            Option syrup = createOption(1L, "바닐라 시럽", OptionCategory.SYRUP);
            when(optionRepository.findAllWithNutritionByBrandIdAndCategory(1L, OptionCategory.SYRUP))
                    .thenReturn(List.of(syrup));

            // when
            List<Option> result = optionService.findBrandOptions(1L, OptionCategory.SYRUP);

            // then
            assertEquals(1, result.size());
            assertEquals("바닐라 시럽", result.get(0).getName());
            verify(optionRepository).findAllWithNutritionByBrandIdAndCategory(1L, OptionCategory.SYRUP);
            verify(optionRepository, never()).findAllWithNutritionByBrandId(any());
        }

        @Test
        @DisplayName("결과 없음 — 빈 리스트 반환")
        void findOptionsEmpty() {
            // given
            when(optionRepository.findAllWithNutritionByBrandId(1L))
                    .thenReturn(Collections.emptyList());

            // when
            List<Option> result = optionService.findBrandOptions(1L, null);

            // then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("brandId가 null — 예외 발생")
        void findOptionsNullBrandId() {
            // given & when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> optionService.findBrandOptions(null, null));

            assertEquals("brandId is invalid", ex.getMessage());
        }

        @Test
        @DisplayName("brandId가 0 이하 — 예외 발생")
        void findOptionsInvalidBrandId() {
            // given & when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> optionService.findBrandOptions(0L, OptionCategory.SYRUP));

            assertEquals("brandId is invalid", ex.getMessage());
        }
    }
}
