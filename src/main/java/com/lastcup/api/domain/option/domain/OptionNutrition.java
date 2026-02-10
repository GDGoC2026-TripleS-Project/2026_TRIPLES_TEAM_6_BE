package com.lastcup.api.domain.option.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "option_nutrition")
public class OptionNutrition {

    @Id
    private Long optionId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Option option;

    @Column(name = "caffeine_mg", nullable = false)
    private int caffeineMg;

    protected OptionNutrition() {
    }

    private OptionNutrition(Option option, int caffeineMg) {
        validateOption(option);
        validateCaffeine(caffeineMg);
        this.option = option;
        this.caffeineMg = caffeineMg;
    }

    public static OptionNutrition create(Option option, int caffeineMg) {
        return new OptionNutrition(option, caffeineMg);
    }

    private static void validateOption(Option option) {
        if (option == null) {
            throw new IllegalArgumentException("option is null");
        }
    }

    private static void validateCaffeine(int caffeineMg) {
        if (caffeineMg < 0) {
            throw new IllegalArgumentException("caffeineMg is invalid");
        }
    }

    public Long getOptionId() {
        return optionId;
    }

    public Option getOption() {
        return option;
    }

    public int getCaffeineMg() {
        return caffeineMg;
    }
}
