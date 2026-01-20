package com.lastcup.api.domain.menu.domain;

import com.lastcup.api.global.config.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "menu_size",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_menu_size", columnNames = {"menu_temperature_id", "size_name"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuSize extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_temperature_id", nullable = false)
    private MenuTemperature menuTemperature;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nutrition_id", nullable = false)
    private Nutrition nutrition;

    @Column(name = "size_name", nullable = false, length = 50)
    private String sizeName;

    private Integer volumeMl;

    public Long getId() {
        return id;
    }

    public MenuTemperature getMenuTemperature() {
        return menuTemperature;
    }

    public Nutrition getNutrition() {
        return nutrition;
    }

    public String getSizeName() {
        return sizeName;
    }

    public Integer getVolumeMl() {
        return volumeMl;
    }
}
