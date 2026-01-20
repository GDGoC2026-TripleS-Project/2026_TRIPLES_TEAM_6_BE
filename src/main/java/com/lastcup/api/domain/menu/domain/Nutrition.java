package com.lastcup.api.domain.menu.domain;

import com.lastcup.api.global.config.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "nutrition")
public class Nutrition extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer caffeineMg;
    private Integer sugarG;
    private Integer calories;
    private Integer sodiumMg;
    private Integer proteinG;
    private Integer fatG;

    protected Nutrition() {
    }

    public Long getId() {
        return id;
    }

    public Integer getCaffeineMg() {
        return caffeineMg;
    }

    public Integer getSugarG() {
        return sugarG;
    }

    public Integer getCalories() {
        return calories;
    }

    public Integer getSodiumMg() {
        return sodiumMg;
    }

    public Integer getProteinG() {
        return proteinG;
    }

    public Integer getFatG() {
        return fatG;
    }
}
