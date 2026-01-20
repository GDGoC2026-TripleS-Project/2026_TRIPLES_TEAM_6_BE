package com.lastcup.api.domain.menu.domain;

import com.lastcup.api.global.config.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "menu_temperature",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_menu_temperature", columnNames = {"menu_id", "temperature"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuTemperature extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TemperatureType temperature;

    @Column(nullable = false)
    private boolean isActive;

    public MenuTemperature(Menu menu, TemperatureType temperature) {
        this.menu = menu;
        this.temperature = temperature;
        this.isActive = true;
    }

    public Long getId() {
        return id;
    }

    public Menu getMenu() {
        return menu;
    }

    public TemperatureType getTemperature() {
        return temperature;
    }

    public boolean isActive() {
        return isActive;
    }
}
