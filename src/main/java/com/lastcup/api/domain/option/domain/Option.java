package com.lastcup.api.domain.option.domain;

import com.lastcup.api.global.config.BaseTimeEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

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

    @Column(nullable = false)
    private int caffeineMg;

    @Column(nullable = false)
    private int sugarG;

    private Integer calories;
    private Integer sodiumMg;
    private Integer proteinG;
    private Integer fatG;

    @Column(length = 30)
    private String displayUnitName;

    @Column(precision = 5, scale = 2)
    private BigDecimal sugarCubeEquivalent;

    protected Option() {
    }

    private Option(
            Long brandId,
            String name,
            OptionCategory category,
            OptionSelectionType selectionType,
            int caffeineMg,
            int sugarG,
            Integer calories,
            Integer sodiumMg,
            Integer proteinG,
            Integer fatG,
            String displayUnitName,
            BigDecimal sugarCubeEquivalent
    ) {
        this.brandId = brandId;
        this.name = name;
        this.category = category;
        this.selectionType = selectionType;
        this.caffeineMg = caffeineMg;
        this.sugarG = sugarG;
        this.calories = calories;
        this.sodiumMg = sodiumMg;
        this.proteinG = proteinG;
        this.fatG = fatG;
        this.displayUnitName = displayUnitName;
        this.sugarCubeEquivalent = sugarCubeEquivalent;
    }

    public static Option create(
            Long brandId,
            String name,
            OptionCategory category,
            OptionSelectionType selectionType,
            int caffeineMg,
            int sugarG,
            Integer calories,
            Integer sodiumMg,
            Integer proteinG,
            Integer fatG,
            String displayUnitName,
            BigDecimal sugarCubeEquivalent
    ) {
        validateBrandId(brandId);
        validateName(name);
        validateCategory(category);
        validateSelectionType(selectionType);

        return new Option(
                brandId,
                name,
                category,
                selectionType,
                caffeineMg,
                sugarG,
                calories,
                sodiumMg,
                proteinG,
                fatG,
                displayUnitName,
                sugarCubeEquivalent
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

    public Long getId() {return id;}

    public Long getBrandId() {return brandId;}

    public String getName() {return name;}

    public OptionCategory getCategory() {return category;}

    public OptionSelectionType getSelectionType() {return selectionType;}

    public int getCaffeineMg() {return caffeineMg;}

    public int getSugarG() {return sugarG;}

    public Integer getCalories() {return calories;}

    public Integer getSodiumMg() {return sodiumMg;}

    public Integer getProteinG() {return proteinG;}

    public Integer getFatG() {return fatG;}

    public String getDisplayUnitName() {return displayUnitName;}

    public BigDecimal getSugarCubeEquivalent() {return sugarCubeEquivalent;}
}