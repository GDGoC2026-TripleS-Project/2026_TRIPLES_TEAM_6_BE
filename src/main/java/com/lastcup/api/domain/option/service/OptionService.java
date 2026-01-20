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

    @Transactional(readOnly = true)
    public List<Option> findBrandOptions(Long brandId, OptionCategory category) {
        validateBrandId(brandId);

        if (category == null) {
            return optionRepository.findByBrandId(brandId);
        }

        return optionRepository.findByBrandIdAndCategory(brandId, category);
    }

    private void validateBrandId(Long brandId) {
        if (brandId == null || brandId <= 0) {
            throw new IllegalArgumentException("brandId is invalid");
        }
    }
}
