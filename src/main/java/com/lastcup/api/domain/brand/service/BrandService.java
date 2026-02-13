package com.lastcup.api.domain.brand.service;

import com.lastcup.api.domain.brand.domain.Brand;
import com.lastcup.api.domain.brand.dto.response.BrandResponse;
import com.lastcup.api.domain.brand.repository.BrandRepository;
import com.lastcup.api.domain.user.repository.BrandFavoriteRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository brandRepository;
    private final BrandFavoriteRepository brandFavoriteRepository;

    public BrandService(BrandRepository brandRepository, BrandFavoriteRepository brandFavoriteRepository) {
        this.brandRepository = brandRepository;
        this.brandFavoriteRepository = brandFavoriteRepository;
    }

    public List<BrandResponse> findBrands(String keyword, Long userId) {
        List<Brand> brands = findBrandsByKeyword(keyword);
        Set<Long> favoriteBrandIds = findFavoriteBrandIds(userId);

        List<BrandResponse> responses = brands.stream()
                .map(brand -> toResponse(brand, favoriteBrandIds))
                .collect(Collectors.toList());

        sortByFavoriteThenName(userId, responses);
        return responses;
    }

    private List<Brand> findBrandsByKeyword(String keyword) {
        if (isBlank(keyword)) {
            return brandRepository.findAllByOrderByIdAsc();
        }
        return brandRepository.findByNameContainingIgnoreCaseOrderByIdAsc(keyword);
    }

    private Set<Long> findFavoriteBrandIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        return brandFavoriteRepository.findBrandIdsByUserId(userId).stream()
                .collect(Collectors.toSet());
    }

    private BrandResponse toResponse(Brand brand, Set<Long> favoriteBrandIds) {
        boolean isFavorite = favoriteBrandIds.contains(brand.getId());
        return new BrandResponse(brand.getId(), brand.getName(), brand.getLogoUrl(), isFavorite);
    }

    private void sortByFavoriteThenName(Long userId, List<BrandResponse> responses) {
        if (userId == null) {
            return;
        }

        responses.sort(
                Comparator.comparing(BrandResponse::isFavorite).reversed()
                        .thenComparing(BrandResponse::id)
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
