package com.lastcup.api.domain.intake.domain;

import com.lastcup.api.global.config.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "intake")
public class Intake extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate intakeDate;

    @Column(nullable = false)
    private Long menuSizeId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int caffeineSnapshot;

    @Column(nullable = false)
    private int sugarSnapshot;

    private Integer caloriesSnapshot;
    private Integer sodiumSnapshot;
    private Integer proteinSnapshot;
    private Integer fatSnapshot;

    @Column(nullable = false)
    private int goalCaffeineTargetSnapshot;

    @Column(nullable = false)
    private int goalSugarTargetSnapshot;

    private Boolean caffeineSuccess;
    private Boolean sugarSuccess;
    private Boolean overallSuccess;
    private LocalDateTime evaluatedAt;

    @OneToMany(mappedBy = "intake", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IntakeOption> intakeOptions = new ArrayList<>();

    protected Intake() {
    }

    private Intake(
            Long userId,
            LocalDate intakeDate,
            Long menuSizeId,
            int quantity,
            int caffeineSnapshot,
            int sugarSnapshot,
            Integer caloriesSnapshot,
            Integer sodiumSnapshot,
            Integer proteinSnapshot,
            Integer fatSnapshot,
            int goalCaffeineTargetSnapshot,
            int goalSugarTargetSnapshot
    ) {
        this.userId = userId;
        this.intakeDate = intakeDate;
        this.menuSizeId = menuSizeId;
        this.quantity = quantity;
        this.caffeineSnapshot = caffeineSnapshot;
        this.sugarSnapshot = sugarSnapshot;
        this.caloriesSnapshot = caloriesSnapshot;
        this.sodiumSnapshot = sodiumSnapshot;
        this.proteinSnapshot = proteinSnapshot;
        this.fatSnapshot = fatSnapshot;
        this.goalCaffeineTargetSnapshot = goalCaffeineTargetSnapshot;
        this.goalSugarTargetSnapshot = goalSugarTargetSnapshot;
    }

    public static Intake create(
            Long userId,
            LocalDate intakeDate,
            Long menuSizeId,
            int quantity,
            int caffeineSnapshot,
            int sugarSnapshot,
            Integer caloriesSnapshot,
            Integer sodiumSnapshot,
            Integer proteinSnapshot,
            Integer fatSnapshot,
            int goalCaffeineTargetSnapshot,
            int goalSugarTargetSnapshot
    ) {
        validateUserId(userId);
        validateIntakeDate(intakeDate);
        validateMenuSizeId(menuSizeId);
        validateQuantity(quantity);

        return new Intake(
                userId, intakeDate, menuSizeId, quantity,
                caffeineSnapshot, sugarSnapshot,
                caloriesSnapshot, sodiumSnapshot, proteinSnapshot, fatSnapshot,
                goalCaffeineTargetSnapshot, goalSugarTargetSnapshot
        );
    }

    public void addOption(Long optionId, int quantity) {
        IntakeOption option = IntakeOption.create(this, optionId, quantity);
        this.intakeOptions.add(option);
    }

    private static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is invalid");
        }
    }

    private static void validateIntakeDate(LocalDate intakeDate) {
        if (intakeDate == null) {
            throw new IllegalArgumentException("intakeDate is null");
        }
        if (intakeDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("intakeDate cannot be in the future");
        }
    }

    private static void validateMenuSizeId(Long menuSizeId) {
        if (menuSizeId == null || menuSizeId <= 0) {
            throw new IllegalArgumentException("menuSizeId is invalid");
        }
    }

    private static void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDate getIntakeDate() {
        return intakeDate;
    }

    public Long getMenuSizeId() {
        return menuSizeId;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getCaffeineSnapshot() {
        return caffeineSnapshot;
    }

    public int getSugarSnapshot() {
        return sugarSnapshot;
    }

    public Integer getCaloriesSnapshot() {
        return caloriesSnapshot;
    }

    public Integer getSodiumSnapshot() {
        return sodiumSnapshot;
    }

    public Integer getProteinSnapshot() {
        return proteinSnapshot;
    }

    public Integer getFatSnapshot() {
        return fatSnapshot;
    }

    public int getGoalCaffeineTargetSnapshot() {
        return goalCaffeineTargetSnapshot;
    }

    public int getGoalSugarTargetSnapshot() {
        return goalSugarTargetSnapshot;
    }

    public Boolean getCaffeineSuccess() {
        return caffeineSuccess;
    }

    public Boolean getSugarSuccess() {
        return sugarSuccess;
    }

    public Boolean getOverallSuccess() {
        return overallSuccess;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public List<IntakeOption> getIntakeOptions() {
        return intakeOptions;
    }
}
