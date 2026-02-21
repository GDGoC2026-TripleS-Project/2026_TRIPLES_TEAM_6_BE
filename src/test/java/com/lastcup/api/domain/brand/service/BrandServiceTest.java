package com.lastcup.api.domain.brand.service;

import com.lastcup.api.domain.brand.domain.Brand;
import com.lastcup.api.domain.brand.dto.response.BrandResponse;
import com.lastcup.api.domain.brand.repository.BrandRepository;
import com.lastcup.api.domain.user.repository.BrandFavoriteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * BrandService 단위 테스트.
 * 키워드 검색 분기, 즐겨찾기 반영, 정렬 로직을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock private BrandRepository brandRepository;
    @Mock private BrandFavoriteRepository brandFavoriteRepository;

    @InjectMocks
    private BrandService brandService;

    // ── 테스트 픽스처 헬퍼 ──

    /**
     * Brand 엔티티를 생성하고, ReflectionTestUtils로 JPA @Id를 설정한다.
     * @GeneratedValue(IDENTITY)는 DB가 할당하므로 테스트에서는 리플렉션으로 주입.
     */
    private Brand createBrand(Long id, String name, String logoUrl) {
        Brand brand = new Brand(name, logoUrl);
        ReflectionTestUtils.setField(brand, "id", id);
        return brand;
    }

    // 1. findBrands — 브랜드 목록 조회
    // keyword 유무 → 전체조회/검색 분기
    // userId 유무 → 즐겨찾기 반영/미반영 분기
    // 즐겨찾기가 있으면 즐겨찾기 우선 정렬

    @Nested
    @DisplayName("브랜드 목록 조회 (findBrands)")
    class FindBrands {

        @Test
        @DisplayName("키워드 null + 비로그인 — 전체 조회, 즐겨찾기 없음")
        void findBrandsNoKeywordNoUser() {
            // given — 키워드가 null이면 findAllByOrderByIdAsc() 호출
            Brand starbucks = createBrand(1L, "스타벅스", "https://logo/starbucks.png");
            Brand twosome = createBrand(2L, "투썸", "https://logo/twosome.png");
            when(brandRepository.findAllByOrderByIdAsc()).thenReturn(List.of(starbucks, twosome));

            // when — userId null = 비로그인
            List<BrandResponse> result = brandService.findBrands(null, null);

            // then — 전체 조회 + 즐겨찾기 모두 false
            assertEquals(2, result.size());
            assertEquals("스타벅스", result.get(0).name());
            assertFalse(result.get(0).isFavorite());
            assertFalse(result.get(1).isFavorite());

            // 행위 검증: 전체 조회 메서드가 호출되었는지 확인
            verify(brandRepository).findAllByOrderByIdAsc();
            verify(brandRepository, never()).findByNameContainingIgnoreCaseOrderByIdAsc(any());
        }

        @Test
        @DisplayName("빈 문자열 키워드 — 전체 조회로 분기 (isBlank 체크)")
        void findBrandsBlankKeyword() {
            // given — 빈 문자열("")도 isBlank()로 null과 동일하게 처리
            when(brandRepository.findAllByOrderByIdAsc()).thenReturn(Collections.emptyList());

            // when
            List<BrandResponse> result = brandService.findBrands("  ", null);

            // then
            assertTrue(result.isEmpty());
            verify(brandRepository).findAllByOrderByIdAsc();
        }

        @Test
        @DisplayName("키워드 있음 — 키워드 검색 분기")
        void findBrandsWithKeyword() {
            // given — 키워드가 있으면 findByNameContainingIgnoreCase 호출
            Brand starbucks = createBrand(1L, "스타벅스", "https://logo/starbucks.png");
            when(brandRepository.findByNameContainingIgnoreCaseOrderByIdAsc("스타"))
                    .thenReturn(List.of(starbucks));

            // when
            List<BrandResponse> result = brandService.findBrands("스타", null);

            // then
            assertEquals(1, result.size());
            assertEquals("스타벅스", result.get(0).name());
            verify(brandRepository).findByNameContainingIgnoreCaseOrderByIdAsc("스타");
            verify(brandRepository, never()).findAllByOrderByIdAsc();
        }

        @Test
        @DisplayName("로그인 유저 + 즐겨찾기 — 즐겨찾기 브랜드 isFavorite=true")
        void findBrandsWithFavorite() {
            // given — userId가 있으면 즐겨찾기 ID 목록을 조회
            Brand starbucks = createBrand(1L, "스타벅스", "https://logo/starbucks.png");
            Brand twosome = createBrand(2L, "투썸", "https://logo/twosome.png");
            when(brandRepository.findAllByOrderByIdAsc())
                    .thenReturn(List.of(starbucks, twosome));
            // userId=100의 즐겨찾기: 스타벅스(1L)만 등록
            when(brandFavoriteRepository.findBrandIdsByUserId(100L))
                    .thenReturn(List.of(1L));

            // when
            List<BrandResponse> result = brandService.findBrands(null, 100L);

            // then — 스타벅스만 isFavorite=true
            assertEquals(2, result.size());
            // 즐겨찾기 우선 정렬이므로 스타벅스가 먼저
            assertTrue(result.get(0).isFavorite());
            assertEquals("스타벅스", result.get(0).name());
            assertFalse(result.get(1).isFavorite());
        }

        @Test
        @DisplayName("로그인 유저 + 즐겨찾기 — 즐겨찾기 우선 → ID 오름차순 정렬")
        void findBrandsSortedByFavoriteThenId() {
            // given — 3개 브랜드 중 ID 3만 즐겨찾기
            Brand b1 = createBrand(1L, "A브랜드", "url1");
            Brand b2 = createBrand(2L, "B브랜드", "url2");
            Brand b3 = createBrand(3L, "C브랜드", "url3");
            when(brandRepository.findAllByOrderByIdAsc())
                    .thenReturn(List.of(b1, b2, b3));
            when(brandFavoriteRepository.findBrandIdsByUserId(100L))
                    .thenReturn(List.of(3L));

            // when
            List<BrandResponse> result = brandService.findBrands(null, 100L);

            // then — C브랜드(즐겨찾기)가 맨 앞, 나머지는 ID 순
            assertEquals(3, result.size());
            assertEquals("C브랜드", result.get(0).name());   // 즐겨찾기 → 우선
            assertTrue(result.get(0).isFavorite());
            assertEquals("A브랜드", result.get(1).name());   // ID 1
            assertEquals("B브랜드", result.get(2).name());   // ID 2
        }

        @Test
        @DisplayName("검색 결과 없음 — 빈 리스트 반환")
        void findBrandsEmptyResult() {
            // given
            when(brandRepository.findByNameContainingIgnoreCaseOrderByIdAsc("없는브랜드"))
                    .thenReturn(Collections.emptyList());

            // when
            List<BrandResponse> result = brandService.findBrands("없는브랜드", null);

            // then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("비로그인 유저 — 정렬 로직 스킵 (userId null이면 sortByFavoriteThenName early return)")
        void findBrandsNoUserNoSort() {
            // given — userId가 null이면 sortByFavoriteThenName()에서 early return
            Brand b1 = createBrand(1L, "A브랜드", "url1");
            Brand b2 = createBrand(2L, "B브랜드", "url2");
            when(brandRepository.findAllByOrderByIdAsc())
                    .thenReturn(List.of(b1, b2));

            // when
            List<BrandResponse> result = brandService.findBrands(null, null);

            // then — DB 조회 순서(ID 오름차순) 그대로 유지
            assertEquals("A브랜드", result.get(0).name());
            assertEquals("B브랜드", result.get(1).name());
            // 즐겨찾기 조회가 호출되지 않아야 한다 (불필요한 쿼리 방지)
            verify(brandFavoriteRepository, never()).findBrandIdsByUserId(any());
        }
    }
}
