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
import static com.lastcup.api.global.config.AppTimeZone.KST;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "intake")
public class Intake extends BaseTimeEntity {

    /** 에스프레소 1잔 기준 카페인(mg) */
    public static final double CAFFEINE_MG_PER_ESPRESSO_SHOT = 75.0;
    /** 각설탕 1개 기준 당류(g) */
    public static final double SUGAR_G_PER_SUGAR_CUBE = 3.0;

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

    /** 카페인을 에스프레소 잔 수로 환산한 값 (75mg = 1잔, 반올림) */
    @Column(nullable = false)
    private int espressoShotCount;

    /** 당류를 각설탕 개수로 환산한 값 (3g = 1개, 반올림) */
    @Column(nullable = false)
    private int sugarCubeCount;

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
        this.espressoShotCount = toEspressoShotCount(caffeineSnapshot);
        this.sugarCubeCount = toSugarCubeCount(sugarSnapshot);
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

    /**
     * 섭취 기록을 다른 음료/수량으로 수정한다.
     * 영양 스냅샷은 Service가 새 MenuSize 기준으로 계산하여 전달한다.
     */
    public void update(
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
        validateIntakeDate(intakeDate);
        validateMenuSizeId(menuSizeId);
        validateQuantity(quantity);

        this.intakeDate = intakeDate;
        this.menuSizeId = menuSizeId;
        this.quantity = quantity;
        this.caffeineSnapshot = caffeineSnapshot;
        this.sugarSnapshot = sugarSnapshot;
        this.caloriesSnapshot = caloriesSnapshot;
        this.sodiumSnapshot = sodiumSnapshot;
        this.proteinSnapshot = proteinSnapshot;
        this.fatSnapshot = fatSnapshot;
        this.espressoShotCount = toEspressoShotCount(caffeineSnapshot);
        this.sugarCubeCount = toSugarCubeCount(sugarSnapshot);
        this.goalCaffeineTargetSnapshot = goalCaffeineTargetSnapshot;
        this.goalSugarTargetSnapshot = goalSugarTargetSnapshot;
    }

    /**
     * 기존 옵션을 모두 제거한다.
     * orphanRemoval = true 이므로 clear() 호출 시 DB에서도 삭제된다.
     */
    public void clearOptions() {
        this.intakeOptions.clear();
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
        if (intakeDate.isAfter(LocalDate.now(KST))) {
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

    public int getEspressoShotCount() {
        return espressoShotCount;
    }

    public int getSugarCubeCount() {
        return sugarCubeCount;
    }

    public List<IntakeOption> getIntakeOptions() {
        return intakeOptions;
    }

    // ── 환산 유틸 ──

    /**
     * 카페인(mg)을 에스프레소 잔 수로 환산한다 (75mg = 1잔, 반올림).
     * "에스프레소 약 n잔" 표시에 사용되며, 반올림이 수학적으로 가장 가까운 정수를 제공한다.
     */
    public static int toEspressoShotCount(int caffeineMg) {
        return (int) Math.round(caffeineMg / CAFFEINE_MG_PER_ESPRESSO_SHOT);
    }

    /**
     * 당류(g)를 각설탕 개수로 환산한다 (3g = 1개, 반올림).
     * "각설탕 약 n개" 표시에 사용되며, 반올림이 수학적으로 가장 가까운 정수를 제공한다.
     */
    public static int toSugarCubeCount(int sugarG) {
        return (int) Math.round(sugarG / SUGAR_G_PER_SUGAR_CUBE);
    }
}
