package com.lastcup.api.domain.option.service;

import com.lastcup.api.domain.option.domain.Option;
import com.lastcup.api.domain.option.domain.OptionCategory;
import com.lastcup.api.domain.option.repository.OptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OptionService {

    private final OptionRepository optionRepository;

    public OptionService(OptionRepository optionRepository) {
        this.optionRepository = optionRepository;
    }

    /**
     * 브랜드의 옵션 목록을 영양 정보와 함께 조회한다.
     * OptionResponse에서 옵션별 카페인(mg)을 반환하기 위해 nutrition을 페치 조인한다.
     * 프론트엔드의 확인 페이지에서 옵션 카페인 미리보기 계산에 사용된다.
     */
    @Transactional(readOnly = true)
    public List<Option> findBrandOptions(Long brandId, OptionCategory category) {
        validateBrandId(brandId);

        if (category == null) {
            return optionRepository.findAllWithNutritionByBrandId(brandId);
        }

        return optionRepository.findAllWithNutritionByBrandIdAndCategory(brandId, category);
    }

    private void validateBrandId(Long brandId) {
        if (brandId == null || brandId <= 0) {
            throw new IllegalArgumentException("brandId is invalid");
        }
    }
}
