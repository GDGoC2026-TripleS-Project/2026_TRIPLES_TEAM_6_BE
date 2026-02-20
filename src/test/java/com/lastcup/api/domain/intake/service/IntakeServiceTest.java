package com.lastcup.api.domain.intake.service;

import com.lastcup.api.domain.goal.domain.UserGoal;
import com.lastcup.api.domain.goal.service.UserGoalService;
import com.lastcup.api.domain.intake.domain.Intake;
import com.lastcup.api.domain.intake.dto.request.CreateIntakeRequest;
import com.lastcup.api.domain.intake.dto.request.IntakeOptionRequest;
import com.lastcup.api.domain.intake.dto.request.IntakeUpdateRequest;
import com.lastcup.api.domain.intake.dto.response.DrinkGroupResponse;
import com.lastcup.api.domain.intake.dto.response.IntakeResponse;
import com.lastcup.api.domain.intake.dto.response.PeriodIntakeStatisticsResponse;
import com.lastcup.api.domain.intake.repository.IntakeRepository;
import com.lastcup.api.domain.menu.domain.MenuSize;
import com.lastcup.api.domain.menu.domain.Nutrition;
import com.lastcup.api.domain.menu.repository.MenuSizeRepository;
import com.lastcup.api.domain.option.domain.Option;
import com.lastcup.api.domain.option.domain.OptionCategory;
import com.lastcup.api.domain.option.domain.OptionNutrition;
import com.lastcup.api.domain.option.domain.OptionSelectionType;
import com.lastcup.api.domain.option.repository.OptionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntakeServiceTest {

    @Mock private IntakeRepository intakeRepository;
    @Mock private MenuSizeRepository menuSizeRepository;
    @Mock private OptionRepository optionRepository;
    @Mock private UserGoalService userGoalService;

    @InjectMocks
    private IntakeService intakeService;

    @Test
    @DisplayName("createIntake: quantity가 null이면 1로 저장되고 스냅샷 계산/goal 조회가 수행된다")
    void createIntakeDefaultsQuantityAndCalculatesSnapshots() {
        LocalDate date = LocalDate.of(2026, 1, 10);
        CreateIntakeRequest request = new CreateIntakeRequest(10L, date, null, List.of(new IntakeOptionRequest(100L, 2)));

        MenuSize menuSize = mock(MenuSize.class);
        Nutrition nutrition = mock(Nutrition.class);
        when(menuSizeRepository.findDetailById(10L)).thenReturn(Optional.of(menuSize));
        when(menuSize.getId()).thenReturn(10L);
        when(menuSize.getNutrition()).thenReturn(nutrition);
        when(nutrition.getCaffeineMg()).thenReturn(100);
        when(nutrition.getSugarG()).thenReturn(5);
        when(nutrition.getCalories()).thenReturn(120);
        when(nutrition.getSodiumMg()).thenReturn(10);
        when(nutrition.getProteinG()).thenReturn(2);
        when(nutrition.getFatG()).thenReturn(1);

        Option option = Option.create(1L, "샷추가", OptionCategory.SHOT, OptionSelectionType.COUNT);
        ReflectionTestUtils.setField(option, "id", 100L);
        ReflectionTestUtils.setField(option, "nutrition", OptionNutrition.create(option, 30));
        when(optionRepository.findAllWithNutritionByIdIn(any())).thenReturn(List.of(option));

        UserGoal goal = UserGoal.create(7L, 400, 25, date, null);
        when(userGoalService.findByDate(7L, date)).thenReturn(goal);
        when(intakeRepository.save(any(Intake.class))).thenAnswer(invocation -> {
            Intake intake = invocation.getArgument(0);
            ReflectionTestUtils.setField(intake, "id", 999L);
            return intake;
        });

        IntakeResponse response = intakeService.createIntake(7L, request);

        assertEquals(1, response.quantity());
        assertEquals(160, response.caffeineSnapshot());
        assertEquals(5, response.sugarSnapshot());
        assertEquals(120, response.caloriesSnapshot());
        assertEquals(10, response.sodiumSnapshot());
        assertEquals(2, response.proteinSnapshot());
        assertEquals(1, response.fatSnapshot());
        verify(userGoalService).findByDate(7L, date);
    }

    @Test
    @DisplayName("createIntake: 메뉴사이즈가 없으면 정확한 예외 메시지를 던진다")
    void createIntakeThrowsWhenMenuSizeMissing() {
        CreateIntakeRequest request = new CreateIntakeRequest(777L, LocalDate.of(2026, 1, 10), 1, List.of());
        when(menuSizeRepository.findDetailById(777L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> intakeService.createIntake(1L, request));

        assertEquals("MenuSize not found: 777", ex.getMessage());
    }

    @Test
    @DisplayName("createIntake: 카페인 계산은 메뉴+옵션*수량 공식을 정확히 따른다")
    void createIntakeCalculatesCaffeineExactly() {
        LocalDate date = LocalDate.of(2026, 1, 10);
        CreateIntakeRequest request = new CreateIntakeRequest(10L, date, 2, List.of(new IntakeOptionRequest(100L, 2)));

        MenuSize menuSize = mock(MenuSize.class);
        Nutrition nutrition = mock(Nutrition.class);
        when(menuSizeRepository.findDetailById(10L)).thenReturn(Optional.of(menuSize));
        when(menuSize.getId()).thenReturn(10L);
        when(menuSize.getNutrition()).thenReturn(nutrition);
        when(nutrition.getCaffeineMg()).thenReturn(100);
        when(nutrition.getSugarG()).thenReturn(1);

        Option option = Option.create(1L, "샷추가", OptionCategory.SHOT, OptionSelectionType.COUNT);
        ReflectionTestUtils.setField(option, "id", 100L);
        ReflectionTestUtils.setField(option, "nutrition", OptionNutrition.create(option, 30));
        when(optionRepository.findAllWithNutritionByIdIn(any())).thenReturn(List.of(option));

        when(userGoalService.findByDate(1L, date)).thenReturn(UserGoal.create(1L, 400, 25, date, null));
        when(intakeRepository.save(any(Intake.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IntakeResponse response = intakeService.createIntake(1L, request);

        assertEquals(320, response.caffeineSnapshot());
    }

    @Test
    @DisplayName("updateIntake: 기존 intake가 없으면 정확한 예외 메시지를 던진다")
    void updateIntakeThrowsWhenMissing() {
        when(intakeRepository.findByIdAndUserId(55L, 1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> intakeService.updateIntake(1L, 55L,
                        new IntakeUpdateRequest(1L, LocalDate.of(2026, 1, 1), 1, List.of())));

        assertEquals("Intake not found: 55", ex.getMessage());
    }

    @Test
    @DisplayName("updateIntake: 옵션 clear 이후 flush 호출 및 스냅샷 재계산이 수행된다")
    void updateIntakeClearsFlushesAndRecalculates() {
        LocalDate date = LocalDate.of(2026, 1, 10);
        Intake intake = Intake.create(1L, date, 10L, 1, 100, 5, 10, 20, 30, 40, 400, 25);
        intake.addOption(1L, 1);
        when(intakeRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(intake));

        MenuSize menuSize = mock(MenuSize.class);
        Nutrition nutrition = mock(Nutrition.class);
        when(menuSizeRepository.findDetailById(10L)).thenReturn(Optional.of(menuSize));
        when(menuSize.getId()).thenReturn(10L);
        when(menuSize.getNutrition()).thenReturn(nutrition);
        when(nutrition.getCaffeineMg()).thenReturn(200);
        when(nutrition.getSugarG()).thenReturn(10);

        Option option = Option.create(1L, "샷", OptionCategory.SHOT, OptionSelectionType.COUNT);
        ReflectionTestUtils.setField(option, "id", 2L);
        ReflectionTestUtils.setField(option, "nutrition", OptionNutrition.create(option, 50));
        when(optionRepository.findAllWithNutritionByIdIn(any())).thenReturn(List.of(option));
        when(userGoalService.findByDate(1L, date)).thenReturn(UserGoal.create(1L, 400, 25, date, null));

        IntakeResponse response = intakeService.updateIntake(1L, 5L,
                new IntakeUpdateRequest(10L, date, 2, List.of(new IntakeOptionRequest(2L, 1))));

        verify(intakeRepository).flush();
        assertEquals(500, response.caffeineSnapshot());
        assertEquals(20, response.sugarSnapshot());
    }

    @Test
    @DisplayName("findPeriodIntakeStatistics: startDate가 endDate보다 뒤면 예외")
    void findPeriodIntakeStatisticsThrowsWhenInvalidPeriod() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> intakeService.findPeriodIntakeStatistics(1L,
                        LocalDate.of(2026, 1, 11), LocalDate.of(2026, 1, 10)));
        assertEquals("startDate cannot be after endDate", ex.getMessage());
    }
}
