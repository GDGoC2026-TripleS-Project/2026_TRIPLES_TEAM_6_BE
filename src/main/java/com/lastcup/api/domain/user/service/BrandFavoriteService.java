package com.lastcup.api.domain.user.service;

import com.lastcup.api.domain.user.domain.BrandFavorite;
import com.lastcup.api.domain.user.repository.BrandFavoriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrandFavoriteService {

    private final BrandFavoriteRepository brandFavoriteRepository;

    public BrandFavoriteService(BrandFavoriteRepository brandFavoriteRepository) {
        this.brandFavoriteRepository = brandFavoriteRepository;
    }

    @Transactional
    public void createBrandFavorite(Long userId, Long brandId) {
        validateIds(userId, brandId);

        if (brandFavoriteRepository.existsByUserIdAndBrandId(userId, brandId)) {
            return;
        }

        BrandFavorite favorite = BrandFavorite.create(userId, brandId);
        brandFavoriteRepository.save(favorite);
    }

    @Transactional
    public void deleteBrandFavorite(Long userId, Long brandId) {
        validateIds(userId, brandId);

        brandFavoriteRepository.deleteByUserIdAndBrandId(userId, brandId);
    }

    private void validateIds(Long userId, Long brandId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
        if (brandId == null || brandId <= 0) {
            throw new IllegalArgumentException("brandId is invalid");
        }
    }
}
