package com.lastcup.api.domain.menu.repository;

import com.lastcup.api.domain.menu.domain.MenuTemperature;
import com.lastcup.api.domain.menu.domain.TemperatureType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuTemperatureRepository extends JpaRepository<MenuTemperature, Long> {

    List<MenuTemperature> findByMenuIdAndIsActiveTrue(Long menuId);

    Optional<MenuTemperature> findByMenuIdAndTemperatureAndIsActiveTrue(Long menuId, TemperatureType temperature);
}
