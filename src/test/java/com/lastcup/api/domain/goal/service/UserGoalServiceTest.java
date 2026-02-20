package com.lastcup.api.domain.goal.service;

import com.lastcup.api.domain.goal.domain.UserGoal;
import com.lastcup.api.domain.goal.repository.UserGoalRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserGoalServiceTest {

    @Mock private UserGoalRepository userGoalRepository;

    @InjectMocks
    private UserGoalService userGoalService;

    @Test
    @DisplayName("findByDate: active goal이 있으면 그대로 반환한다")
    void findByDateReturnsActiveGoal() {
        LocalDate date = LocalDate.of(2026, 1, 10);
        UserGoal active = UserGoal.create(1L, 300, 20, date.minusDays(1), null);
        when(userGoalRepository.findActiveByDate(eq(1L), eq(date), any(Pageable.class)))
                .thenReturn(List.of(active));

        UserGoal result = userGoalService.findByDate(1L, date);

        assertSame(active, result);
        verify(userGoalRepository, never()).save(any());
    }

    @Test
    @DisplayName("findByDate: active goal이 없으면 DEFAULT 목표를 생성 저장한다")
    void findByDateCreatesDefaultWhenMissing() {
        LocalDate date = LocalDate.of(2026, 1, 10);
        when(userGoalRepository.findActiveByDate(eq(1L), eq(date), any(Pageable.class)))
                .thenReturn(List.of());
        when(userGoalRepository.save(any(UserGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserGoal result = userGoalService.findByDate(1L, date);

        assertEquals(UserGoalService.DEFAULT_DAILY_CAFFEINE_TARGET, result.getDailyCaffeineTarget());
        assertEquals(UserGoalService.DEFAULT_DAILY_SUGAR_TARGET, result.getDailySugarTarget());
        verify(userGoalRepository).save(any(UserGoal.class));
    }

    @Test
    @DisplayName("updateGoal: current가 null이면 새 goal을 저장한다")
    void updateGoalCreatesWhenCurrentNull() {
        LocalDate start = LocalDate.of(2026, 1, 10);
        when(userGoalRepository.findTopByUserIdAndEndDateIsNullOrderByStartDateDesc(1L)).thenReturn(Optional.empty());
        when(userGoalRepository.save(any(UserGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserGoal result = userGoalService.updateGoal(1L, 350, 15, start);

        assertEquals(350, result.getDailyCaffeineTarget());
        assertEquals(15, result.getDailySugarTarget());
        assertEquals(start, result.getStartDate());
        verify(userGoalRepository).save(any(UserGoal.class));
    }

    @Test
    @DisplayName("updateGoal: startDate가 current.startDate와 같으면 updateTargets만 수행한다")
    void updateGoalUpdatesCurrentWhenSameStartDate() {
        LocalDate start = LocalDate.of(2026, 1, 10);
        UserGoal current = spy(UserGoal.create(1L, 300, 20, start, null));
        when(userGoalRepository.findTopByUserIdAndEndDateIsNullOrderByStartDateDesc(1L)).thenReturn(Optional.of(current));

        UserGoal result = userGoalService.updateGoal(1L, 400, 10, start);

        verify(current).updateTargets(400, 10);
        verify(userGoalRepository, never()).save(any(UserGoal.class));
        assertSame(current, result);
    }

    @Test
    @DisplayName("updateGoal: effectiveStartDate가 current.startDate보다 이르면 예외")
    void updateGoalThrowsWhenStartDateBeforeCurrent() {
        UserGoal current = UserGoal.create(1L, 300, 20, LocalDate.of(2026, 1, 10), null);
        when(userGoalRepository.findTopByUserIdAndEndDateIsNullOrderByStartDateDesc(1L)).thenReturn(Optional.of(current));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userGoalService.updateGoal(1L, 400, 10, LocalDate.of(2026, 1, 9)));

        assertEquals("startDate is before current startDate", ex.getMessage());
    }

    @Test
    @DisplayName("updateGoal: startDate가 더 늦으면 current.close(start-1) 후 next 저장")
    void updateGoalClosesCurrentAndSavesNext() {
        UserGoal current = spy(UserGoal.create(1L, 300, 20, LocalDate.of(2026, 1, 10), null));
        LocalDate nextStart = LocalDate.of(2026, 1, 15);
        when(userGoalRepository.findTopByUserIdAndEndDateIsNullOrderByStartDateDesc(1L)).thenReturn(Optional.of(current));
        when(userGoalRepository.save(any(UserGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserGoal result = userGoalService.updateGoal(1L, 450, 12, nextStart);

        verify(current).close(LocalDate.of(2026, 1, 14));
        verify(userGoalRepository).save(any(UserGoal.class));
        assertEquals(nextStart, result.getStartDate());
        assertNull(result.getEndDate());
    }
}
