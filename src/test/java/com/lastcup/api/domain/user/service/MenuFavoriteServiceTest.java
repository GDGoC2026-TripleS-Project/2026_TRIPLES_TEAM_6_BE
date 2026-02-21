package com.lastcup.api.domain.user.service;

import com.lastcup.api.domain.user.domain.MenuFavorite;
import com.lastcup.api.domain.user.repository.MenuFavoriteRepository;
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
 * MenuFavoriteService 단위 테스트.
 * 메뉴 즐겨찾기 등록/삭제, 중복 방지, ID 유효성 검증을 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class MenuFavoriteServiceTest {

    @Mock private MenuFavoriteRepository menuFavoriteRepository;

    @InjectMocks
    private MenuFavoriteService menuFavoriteService;

    // 1. createMenuFavorite — 메뉴 즐겨찾기 등록

    @Nested
    @DisplayName("메뉴 즐겨찾기 등록 (createMenuFavorite)")
    class CreateMenuFavorite {

        @Test
        @DisplayName("정상 등록 — 신규 즐겨찾기 저장")
        void createSuccess() {
            // given
            when(menuFavoriteRepository.existsByUserIdAndMenuId(1L, 20L)).thenReturn(false);

            // when
            menuFavoriteService.createMenuFavorite(1L, 20L);

            // then
            verify(menuFavoriteRepository).save(any(MenuFavorite.class));
        }

        @Test
        @DisplayName("이미 존재 — 저장 스킵 (멱등성)")
        void createAlreadyExists() {
            // given
            when(menuFavoriteRepository.existsByUserIdAndMenuId(1L, 20L)).thenReturn(true);

            // when
            menuFavoriteService.createMenuFavorite(1L, 20L);

            // then
            verify(menuFavoriteRepository, never()).save(any());
        }

        @Test
        @DisplayName("userId가 null — 예외 발생")
        void createNullUserId() {
            // given & when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> menuFavoriteService.createMenuFavorite(null, 20L));

            assertEquals("userId is invalid", ex.getMessage());
        }

        @Test
        @DisplayName("menuId가 0 이하 — 예외 발생")
        void createInvalidMenuId() {
            // given & when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> menuFavoriteService.createMenuFavorite(1L, 0L));

            assertEquals("menuId is invalid", ex.getMessage());
        }
    }

    // 2. deleteMenuFavorite — 메뉴 즐겨찾기 삭제

    @Nested
    @DisplayName("메뉴 즐겨찾기 삭제 (deleteMenuFavorite)")
    class DeleteMenuFavorite {

        @Test
        @DisplayName("정상 삭제")
        void deleteSuccess() {
            // given & when
            menuFavoriteService.deleteMenuFavorite(1L, 20L);

            // then
            verify(menuFavoriteRepository).deleteByUserIdAndMenuId(1L, 20L);
        }

        @Test
        @DisplayName("userId가 null — 예외 발생")
        void deleteNullUserId() {
            // given & when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> menuFavoriteService.deleteMenuFavorite(null, 20L));

            assertEquals("userId is invalid", ex.getMessage());
            verify(menuFavoriteRepository, never()).deleteByUserIdAndMenuId(any(), any());
        }
    }
}
