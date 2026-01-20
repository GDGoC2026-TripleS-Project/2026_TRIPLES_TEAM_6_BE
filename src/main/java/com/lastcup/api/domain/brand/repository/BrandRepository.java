package com.lastcup.api.domain.brand.repository;

import com.lastcup.api.domain.brand.domain.Brand;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    List<Brand> findByNameContainingIgnoreCaseOrderByNameAsc(String keyword);

    List<Brand> findAllByOrderByNameAsc();
}
