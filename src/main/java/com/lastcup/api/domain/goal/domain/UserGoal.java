package com.lastcup.api.domain.goal.domain;

import com.lastcup.api.global.config.BaseTimeEntity;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "user_goals")
public class UserGoal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int dailyCaffeineTarget;

    @Column(nullable = false)
    private int dailySugarTarget;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    protected UserGoal() {
    }

    private UserGoal(
            Long userId,
            int dailyCaffeineTarget,
            int dailySugarTarget,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this.userId = userId;
        this.dailyCaffeineTarget = dailyCaffeineTarget;
        this.dailySugarTarget = dailySugarTarget;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static UserGoal create(
            Long userId,
            int dailyCaffeineTarget,
            int dailySugarTarget,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateUserId(userId);
        validateNonNegative(dailyCaffeineTarget, "dailyCaffeineTarget");
        validateNonNegative(dailySugarTarget, "dailySugarTarget");
        validateStartDate(startDate);
        validateEndDate(startDate, endDate);
        return new UserGoal(userId, dailyCaffeineTarget, dailySugarTarget, startDate, endDate);
    }

    public void updateTargets(Integer dailyCaffeineTarget, Integer dailySugarTarget) {
        if (dailyCaffeineTarget != null) {
            validateNonNegative(dailyCaffeineTarget, "dailyCaffeineTarget");
            this.dailyCaffeineTarget = dailyCaffeineTarget;
        }
        if (dailySugarTarget != null) {
            validateNonNegative(dailySugarTarget, "dailySugarTarget");
            this.dailySugarTarget = dailySugarTarget;
        }
    }

    public void close(LocalDate endDate) {
        validateStartDate(this.startDate);
        validateEndDate(this.startDate, endDate);
        this.endDate = endDate;
    }

    private static void validateUserId(Long userId) {
        if (userId != null && userId > 0) {
            return;
        }
        throw new IllegalArgumentException("userId is invalid");
    }

    private static void validateNonNegative(int value, String field) {
        if (value >= 0) {
            return;
        }
        throw new IllegalArgumentException(field + " is negative");
    }

    private static void validateStartDate(LocalDate startDate) {
        if (startDate != null) {
            return;
        }
        throw new IllegalArgumentException("startDate is null");
    }

    private static void validateEndDate(LocalDate startDate, LocalDate endDate) {
        if (endDate == null) {
            return;
        }
        if (!endDate.isBefore(startDate)) {
            return;
        }
        throw new IllegalArgumentException("endDate is before startDate");
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public int getDailyCaffeineTarget() {
        return dailyCaffeineTarget;
    }

    public int getDailySugarTarget() {
        return dailySugarTarget;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
