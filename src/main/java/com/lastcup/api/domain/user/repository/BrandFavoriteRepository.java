package com.lastcup.api.domain.user.repository;

import com.lastcup.api.domain.user.domain.BrandFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BrandFavoriteRepository extends JpaRepository<BrandFavorite, Long> {

    @Query("SELECT bf.brandId FROM BrandFavorite bf WHERE bf.userId = :userId")
    List<Long> findBrandIdsByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndBrandId(Long userId, Long brandId);

    Optional<BrandFavorite> findByUserIdAndBrandId(Long userId, Long brandId);

    void deleteByUserIdAndBrandId(Long userId, Long brandId);
}
