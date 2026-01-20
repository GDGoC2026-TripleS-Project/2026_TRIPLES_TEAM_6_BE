package com.lastcup.api.domain.user.domain;

import com.lastcup.api.global.config.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(
        name = "brand_favorites",
        uniqueConstraints = @UniqueConstraint(name = "uk_brand_favorites_user_brand", columnNames = {"user_id", "brand_id"})
)
public class BrandFavorite extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    protected BrandFavorite() {
    }

    private BrandFavorite(Long userId, Long brandId) {
        this.userId = userId;
        this.brandId = brandId;
    }

    public static BrandFavorite create(Long userId, Long brandId) {
        validateUserId(userId);
        validateBrandId(brandId);

        return new BrandFavorite(userId, brandId);
    }

    private static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
    }

    private static void validateBrandId(Long brandId) {
        if (brandId == null || brandId <= 0) {
            throw new IllegalArgumentException("brandId is invalid");
        }
    }

    public Long getId() {return id;}

    public Long getUserId() {return userId;}

    public Long getBrandId() {return brandId;}
}
