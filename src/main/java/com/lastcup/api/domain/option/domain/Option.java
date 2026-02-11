package com.lastcup.api.domain.option.domain;

import com.lastcup.api.global.config.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "options")
public class Option extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long brandId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OptionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OptionSelectionType selectionType;

    @OneToOne(mappedBy = "option", fetch = FetchType.LAZY)
    private OptionNutrition nutrition;

    protected Option() {
    }

    private Option(
            Long brandId,
            String name,
            OptionCategory category,
            OptionSelectionType selectionType
    ) {
        this.brandId = brandId;
        this.name = name;
        this.category = category;
        this.selectionType = selectionType;
    }

    public static Option create(
            Long brandId,
            String name,
            OptionCategory category,
            OptionSelectionType selectionType
    ) {
        validateBrandId(brandId);
        validateName(name);
        validateCategory(category);
        validateSelectionType(selectionType);

        return new Option(
                brandId,
                name,
                category,
                selectionType
        );
    }

    private static void validateBrandId(Long brandId) {
        if (brandId == null || brandId <= 0) {
            throw new IllegalArgumentException("brandId is invalid");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is blank");
        }
    }

    private static void validateCategory(OptionCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("category is null");
        }
    }

    private static void validateSelectionType(OptionSelectionType selectionType) {
        if (selectionType == null) {
            throw new IllegalArgumentException("selectionType is null");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getBrandId() {
        return brandId;
    }

    public String getName() {
        return name;
    }

    public OptionCategory getCategory() {
        return category;
    }

    public OptionSelectionType getSelectionType() {
        return selectionType;
    }

    public OptionNutrition getNutrition() {
        return nutrition;
    }
}
