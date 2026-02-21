package com.lastcup.api.domain.user.service;

import com.lastcup.api.domain.user.domain.BrandFavorite;
import com.lastcup.api.domain.user.repository.BrandFavoriteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BrandFavoriteService 단위 테스트.
 * 브랜드 즐겨찾기 등록/삭제, 중복 방지, ID 유효성 검증을 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class BrandFavoriteServiceTest {

    @Mock private BrandFavoriteRepository brandFavoriteRepository;

    @InjectMocks
    private BrandFavoriteService brandFavoriteService;

    // 1. createBrandFavorite — 즐겨찾기 등록
    // 흐름: validateIds → existsByUserIdAndBrandId → 있으면 스킵 / 없으면 save

    @Nested
    @DisplayName("브랜드 즐겨찾기 등록 (createBrandFavorite)")
    class CreateBrandFavorite {

        @Test
        @DisplayName("정상 등록 — 신규 즐겨찾기 저장")
        void createSuccess() {
            // given — 아직 즐겨찾기 등록 안 된 상태
            when(brandFavoriteRepository.existsByUserIdAndBrandId(1L, 10L)).thenReturn(false);

            // when
            brandFavoriteService.createBrandFavorite(1L, 10L);

            // then
            verify(brandFavoriteRepository).save(any(BrandFavorite.class));
        }

        @Test
        @DisplayName("이미 존재 — 저장 스킵 (멱등성)")
        void createAlreadyExists() {
            // given — 이미 즐겨찾기에 등록되어 있으면 중복 저장 방지
            when(brandFavoriteRepository.existsByUserIdAndBrandId(1L, 10L)).thenReturn(true);

            // when
            brandFavoriteService.createBrandFavorite(1L, 10L);

            // then — save가 호출되면 안 됨
            verify(brandFavoriteRepository, never()).save(any());
        }

        @Test
        @DisplayName("userId가 null — 예외 발생")
        void createNullUserId() {
            // given & when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> brandFavoriteService.createBrandFavorite(null, 10L));

            assertEquals("userId is invalid", ex.getMessage());
        }

        @Test
        @DisplayName("brandId가 0 이하 — 예외 발생")
        void createInvalidBrandId() {
            // given & when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> brandFavoriteService.createBrandFavorite(1L, 0L));

            assertEquals("brandId is invalid", ex.getMessage());
        }
    }

    // 2. deleteBrandFavorite — 즐겨찾기 삭제

    @Nested
    @DisplayName("브랜드 즐겨찾기 삭제 (deleteBrandFavorite)")
    class DeleteBrandFavorite {

        @Test
        @DisplayName("정상 삭제")
        void deleteSuccess() {
            // given & when
            brandFavoriteService.deleteBrandFavorite(1L, 10L);

            // then — deleteByUserIdAndBrandId가 호출되었는지 검증
            verify(brandFavoriteRepository).deleteByUserIdAndBrandId(1L, 10L);
        }

        @Test
        @DisplayName("userId가 null — 예외 발생")
        void deleteNullUserId() {
            // given & when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> brandFavoriteService.deleteBrandFavorite(null, 10L));

            assertEquals("userId is invalid", ex.getMessage());
            verify(brandFavoriteRepository, never()).deleteByUserIdAndBrandId(any(), any());
        }
    }
}
