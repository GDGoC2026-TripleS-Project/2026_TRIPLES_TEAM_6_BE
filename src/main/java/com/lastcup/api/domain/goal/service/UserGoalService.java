package com.lastcup.api.domain.goal.service;

import static com.lastcup.api.global.config.AppTimeZone.KST;

import com.lastcup.api.domain.goal.domain.UserGoal;
import com.lastcup.api.domain.goal.repository.UserGoalRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserGoalService {

    public static final int DEFAULT_DAILY_CAFFEINE_TARGET = 400;
    public static final int DEFAULT_DAILY_SUGAR_TARGET = 25;

    private final UserGoalRepository userGoalRepository;

    public UserGoalService(UserGoalRepository userGoalRepository) {
        this.userGoalRepository = userGoalRepository;
    }

    @Transactional(readOnly = true)
    public UserGoal findCurrent(Long userId) {
        LocalDate today = LocalDate.now(KST);
        return findFirstActiveByDate(userId, today)
                .orElseThrow(() -> new IllegalArgumentException("user goal not found"));
    }

    public UserGoal findByDate(Long userId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now(KST);
        return findFirstActiveByDate(userId, targetDate)
                .orElseGet(() -> createDefault(userId, targetDate));
    }

    public UserGoal findOrCreateCurrent(Long userId) {
        return findByDate(userId, LocalDate.now(KST));
    }

    @Transactional(readOnly = true)
    public Optional<UserGoal> findOptionalByDate(Long userId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now(KST);
        return findFirstActiveByDate(userId, targetDate);
    }

    public UserGoal updateGoal(
            Long userId,
            int dailyCaffeineTarget,
            int dailySugarTarget,
            LocalDate startDate
    ) {
        LocalDate effectiveStartDate = resolveStartDate(startDate);
        UserGoal current = userGoalRepository.findTopByUserIdAndEndDateIsNullOrderByStartDateDesc(userId)
                .orElse(null);

        if (current == null) {
            return userGoalRepository.save(UserGoal.create(
                    userId,
                    dailyCaffeineTarget,
                    dailySugarTarget,
                    effectiveStartDate,
                    null
            ));
        }

        if (current.getStartDate().isEqual(effectiveStartDate)) {
            current.updateTargets(dailyCaffeineTarget, dailySugarTarget);
            return current;
        }

        if (effectiveStartDate.isBefore(current.getStartDate())) {
            throw new IllegalArgumentException("startDate is before current startDate");
        }

        current.close(effectiveStartDate.minusDays(1));
        UserGoal next = UserGoal.create(
                userId,
                dailyCaffeineTarget,
                dailySugarTarget,
                effectiveStartDate,
                null
        );
        return userGoalRepository.save(next);
    }

    private UserGoal createDefault(Long userId, LocalDate startDate) {
        UserGoal created = UserGoal.create(
                userId,
                DEFAULT_DAILY_CAFFEINE_TARGET,
                DEFAULT_DAILY_SUGAR_TARGET,
                startDate,
                null
        );
        return userGoalRepository.save(created);
    }

    private Optional<UserGoal> findFirstActiveByDate(Long userId, LocalDate date) {
        return userGoalRepository.findActiveByDate(userId, date, PageRequest.of(0, 1))
                .stream()
                .findFirst();
    }

    private LocalDate resolveStartDate(LocalDate startDate) {
        return startDate != null ? startDate : LocalDate.now(KST);
    }
}
