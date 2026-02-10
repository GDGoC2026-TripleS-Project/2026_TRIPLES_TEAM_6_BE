package com.lastcup.api.domain.intake.service;

import com.lastcup.api.domain.goal.domain.UserGoal;
import com.lastcup.api.domain.goal.service.UserGoalService;
import com.lastcup.api.domain.intake.domain.Intake;
import com.lastcup.api.domain.intake.domain.IntakeOption;
import com.lastcup.api.domain.intake.dto.request.CreateIntakeRequest;
import com.lastcup.api.domain.intake.dto.request.IntakeOptionRequest;
import com.lastcup.api.domain.intake.dto.request.IntakeUpdateRequest;
import com.lastcup.api.domain.intake.dto.response.DailyIntakeSummaryResponse;
import com.lastcup.api.domain.intake.dto.response.IntakeDetailResponse;
import com.lastcup.api.domain.intake.dto.response.IntakeHistoryItemResponse;
import com.lastcup.api.domain.intake.dto.response.IntakeOptionDetailResponse;
import com.lastcup.api.domain.intake.dto.response.IntakeOptionResponse;
import com.lastcup.api.domain.intake.dto.response.IntakeResponse;
import com.lastcup.api.domain.intake.dto.response.PeriodIntakeSummaryResponse;
import com.lastcup.api.domain.intake.repository.IntakeRepository;
import com.lastcup.api.domain.menu.domain.Menu;
import com.lastcup.api.domain.menu.domain.MenuSize;
import com.lastcup.api.domain.menu.domain.MenuTemperature;
import com.lastcup.api.domain.menu.domain.Nutrition;
import com.lastcup.api.domain.menu.repository.MenuSizeRepository;
import com.lastcup.api.domain.option.domain.Option;
import com.lastcup.api.domain.option.repository.OptionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IntakeService {

    private final IntakeRepository intakeRepository;
    private final MenuSizeRepository menuSizeRepository;
    private final OptionRepository optionRepository;
    private final UserGoalService userGoalService;

    public IntakeService(
            IntakeRepository intakeRepository,
            MenuSizeRepository menuSizeRepository,
            OptionRepository optionRepository,
            UserGoalService userGoalService
    ) {
        this.intakeRepository = intakeRepository;
        this.menuSizeRepository = menuSizeRepository;
        this.optionRepository = optionRepository;
        this.userGoalService = userGoalService;
    }

    public IntakeResponse createIntake(Long userId, CreateIntakeRequest request) {
        LocalDate intakeDate = resolveIntakeDate(request.intakeDate());
        int quantity = resolveQuantity(request.quantity());

        MenuSize menuSize = findMenuSizeWithNutrition(request.menuSizeId());
        UserGoal goal = userGoalService.findByDate(userId, intakeDate);

        validateOptions(request.options());

        NutritionSnapshots snapshots = calculateNutritionSnapshots(menuSize.getNutrition(), quantity);
        Intake intake = Intake.create(
                userId, intakeDate, menuSize.getId(), quantity,
                snapshots.caffeine(), snapshots.sugar(),
                snapshots.calories(), snapshots.sodium(),
                snapshots.protein(), snapshots.fat(),
                goal.getDailyCaffeineTarget(), goal.getDailySugarTarget()
        );

        addOptions(intake, request.options());

        Intake saved = intakeRepository.save(intake);
        return toResponse(saved);
    }

    /**
     * 섭취 기록 수정: 음료(MenuSize)·날짜·수량·옵션을 변경하고 영양 스냅샷을 재계산한다.
     * 기존 옵션을 모두 제거한 뒤 새 옵션으로 교체한다(전체 교체 방식).
     */
    public IntakeResponse updateIntake(Long userId, Long intakeId, IntakeUpdateRequest request) {
        Intake intake = findIntakeByIdAndUserId(intakeId, userId);

        MenuSize menuSize = findMenuSizeWithNutrition(request.menuSizeId());
        UserGoal goal = userGoalService.findByDate(userId, request.intakeDate());
        int quantity = request.quantity();

        validateOptions(request.options());

        NutritionSnapshots snapshots = calculateNutritionSnapshots(menuSize.getNutrition(), quantity);
        intake.update(
                request.intakeDate(), menuSize.getId(), quantity,
                snapshots.caffeine(), snapshots.sugar(),
                snapshots.calories(), snapshots.sodium(),
                snapshots.protein(), snapshots.fat(),
                goal.getDailyCaffeineTarget(), goal.getDailySugarTarget()
        );

        // 옵션 전체 교체 – flush()로 DELETE 선반영하여 유니크 제약 충돌 방지
        intake.clearOptions();
        intakeRepository.flush();
        addOptions(intake, request.options());

        return toResponse(intake);
    }

    public void deleteIntake(Long userId, Long intakeId) {
        Intake intake = findIntakeByIdAndUserId(intakeId, userId);
        intakeRepository.delete(intake);
    }

    @Transactional(readOnly = true)
    public DailyIntakeSummaryResponse findDailyIntakes(Long userId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();

        List<Intake> intakes = intakeRepository.findDailyIntakes(userId, targetDate);

        Map<Long, MenuSize> menuSizeMap = fetchMenuSizeMap(intakes);
        Map<Long, String> optionNameMap = fetchOptionNameMap(intakes);

        List<IntakeHistoryItemResponse> items = intakes.stream()
                .map(intake -> toHistoryItem(intake, menuSizeMap, optionNameMap))
                .toList();

        int totalCaffeine = intakes.stream().mapToInt(Intake::getCaffeineSnapshot).sum();
        int totalSugar = intakes.stream().mapToInt(Intake::getSugarSnapshot).sum();
        int intakeCount = intakes.stream().mapToInt(Intake::getQuantity).sum();

        Optional<UserGoal> goalOpt = userGoalService.findOptionalByDate(userId, targetDate);
        int goalCaffeine = goalOpt.map(UserGoal::getDailyCaffeineTarget)
                .orElse(UserGoalService.DEFAULT_DAILY_CAFFEINE_TARGET);
        int goalSugar = goalOpt.map(UserGoal::getDailySugarTarget)
                .orElse(UserGoalService.DEFAULT_DAILY_SUGAR_TARGET);

        return new DailyIntakeSummaryResponse(
                targetDate, totalCaffeine, totalSugar,
                Intake.toEspressoShotCount(totalCaffeine),
                Intake.toSugarCubeCount(totalSugar),
                goalCaffeine, goalSugar, intakeCount, items
        );
    }

    @Transactional(readOnly = true)
    public PeriodIntakeSummaryResponse findPeriodIntakes(Long userId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }

        List<Intake> intakes = intakeRepository.findPeriodIntakes(userId, startDate, endDate);

        Map<Long, MenuSize> menuSizeMap = fetchMenuSizeMap(intakes);
        Map<Long, String> optionNameMap = fetchOptionNameMap(intakes);

        List<IntakeHistoryItemResponse> items = intakes.stream()
                .map(intake -> toHistoryItem(intake, menuSizeMap, optionNameMap))
                .toList();

        int totalCaffeine = intakes.stream().mapToInt(Intake::getCaffeineSnapshot).sum();
        int totalSugar = intakes.stream().mapToInt(Intake::getSugarSnapshot).sum();
        int intakeCount = intakes.stream().mapToInt(Intake::getQuantity).sum();

        return new PeriodIntakeSummaryResponse(
                startDate, endDate, totalCaffeine, totalSugar,
                Intake.toEspressoShotCount(totalCaffeine),
                Intake.toSugarCubeCount(totalSugar),
                intakeCount, items
        );
    }

    @Transactional(readOnly = true)
    public IntakeDetailResponse findIntakeDetail(Long userId, Long intakeId) {
        Intake intake = findIntakeByIdAndUserId(intakeId, userId);

        Map<Long, MenuSize> menuSizeMap = fetchMenuSizeMap(List.of(intake));
        Map<Long, String> optionNameMap = fetchOptionNameMap(List.of(intake));

        MenuSize menuSize = menuSizeMap.get(intake.getMenuSizeId());

        // 수정 플로우용 ID (프론트엔드가 기존 선택 화면을 복원하는 데 사용)
        Long brandId = null;
        Long menuId = null;
        String brandName = "";
        String menuName = "";
        String temperature = "";
        String sizeName = "";

        if (menuSize != null) {
            MenuTemperature mt = menuSize.getMenuTemperature();
            Menu menu = mt.getMenu();
            brandId = menu.getBrand().getId();
            menuId = menu.getId();
            brandName = menu.getBrand().getName();
            menuName = menu.getName();
            temperature = mt.getTemperature().name();
            sizeName = menuSize.getSizeName();
        }

        List<IntakeOptionDetailResponse> options = toOptionDetails(intake, optionNameMap);

        return new IntakeDetailResponse(
                intake.getId(),
                intake.getIntakeDate(),
                brandId, menuId, intake.getMenuSizeId(),
                brandName, menuName, temperature, sizeName,
                intake.getQuantity(),
                intake.getCaffeineSnapshot(),
                intake.getSugarSnapshot(),
                intake.getEspressoShotCount(),
                intake.getSugarCubeCount(),
                intake.getCaloriesSnapshot(),
                intake.getSodiumSnapshot(),
                intake.getProteinSnapshot(),
                intake.getFatSnapshot(),
                intake.getGoalCaffeineTargetSnapshot(),
                intake.getGoalSugarTargetSnapshot(),
                options,
                intake.getCreatedAt()
        );
    }

    // ── 공통 조회 ──

    private Intake findIntakeByIdAndUserId(Long intakeId, Long userId) {
        return intakeRepository.findByIdAndUserId(intakeId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Intake not found: " + intakeId));
    }

    // ── 생성·수정 관련 private 메서드 ──

    private MenuSize findMenuSizeWithNutrition(Long menuSizeId) {
        return menuSizeRepository.findDetailById(menuSizeId)
                .orElseThrow(() -> new IllegalArgumentException("MenuSize not found: " + menuSizeId));
    }

    private void validateOptions(List<IntakeOptionRequest> options) {
        if (options == null || options.isEmpty()) {
            return;
        }

        Set<Long> optionIds = options.stream()
                .map(IntakeOptionRequest::optionId)
                .collect(Collectors.toSet());

        long existingCount = optionRepository.countByIdIn(optionIds);
        if (existingCount != optionIds.size()) {
            throw new IllegalArgumentException("Option not found");
        }
    }

    private void addOptions(Intake intake, List<IntakeOptionRequest> options) {
        if (options == null || options.isEmpty()) {
            return;
        }
        for (IntakeOptionRequest option : options) {
            intake.addOption(option.optionId(), option.quantity());
        }
    }

    // ── 조회 관련 private 메서드 ──

    private Map<Long, MenuSize> fetchMenuSizeMap(List<Intake> intakes) {
        Set<Long> menuSizeIds = intakes.stream()
                .map(Intake::getMenuSizeId)
                .collect(Collectors.toSet());

        if (menuSizeIds.isEmpty()) {
            return Map.of();
        }

        return menuSizeRepository.findAllDetailByIds(menuSizeIds).stream()
                .collect(Collectors.toMap(MenuSize::getId, Function.identity()));
    }

    private Map<Long, String> fetchOptionNameMap(List<Intake> intakes) {
        Set<Long> optionIds = intakes.stream()
                .flatMap(i -> i.getIntakeOptions().stream())
                .map(IntakeOption::getOptionId)
                .collect(Collectors.toSet());

        if (optionIds.isEmpty()) {
            return Map.of();
        }

        return optionRepository.findAllById(optionIds).stream()
                .collect(Collectors.toMap(Option::getId, Option::getName));
    }

    private IntakeHistoryItemResponse toHistoryItem(
            Intake intake,
            Map<Long, MenuSize> menuSizeMap,
            Map<Long, String> optionNameMap
    ) {
        MenuSize menuSize = menuSizeMap.get(intake.getMenuSizeId());

        String brandName = "";
        String menuName = "";
        String temperature = "";
        String sizeName = "";

        if (menuSize != null) {
            MenuTemperature mt = menuSize.getMenuTemperature();
            Menu menu = mt.getMenu();
            brandName = menu.getBrand().getName();
            menuName = menu.getName();
            temperature = mt.getTemperature().name();
            sizeName = menuSize.getSizeName();
        }

        List<IntakeOptionDetailResponse> options = toOptionDetails(intake, optionNameMap);

        return new IntakeHistoryItemResponse(
                intake.getId(),
                intake.getIntakeDate(),
                brandName, menuName, temperature, sizeName,
                intake.getCaffeineSnapshot(),
                intake.getSugarSnapshot(),
                intake.getEspressoShotCount(),
                intake.getSugarCubeCount(),
                intake.getQuantity(),
                options,
                intake.getCreatedAt()
        );
    }

    private List<IntakeOptionDetailResponse> toOptionDetails(Intake intake, Map<Long, String> optionNameMap) {
        return intake.getIntakeOptions().stream()
                .map(o -> new IntakeOptionDetailResponse(
                        o.getOptionId(),
                        optionNameMap.getOrDefault(o.getOptionId(), ""),
                        o.getQuantity()
                ))
                .toList();
    }

    // ── 응답 변환 ──

    private IntakeResponse toResponse(Intake intake) {
        List<IntakeOptionResponse> optionResponses = intake.getIntakeOptions().stream()
                .map(o -> new IntakeOptionResponse(o.getOptionId(), o.getQuantity()))
                .toList();

        return new IntakeResponse(
                intake.getId(),
                intake.getUserId(),
                intake.getIntakeDate(),
                intake.getMenuSizeId(),
                intake.getQuantity(),
                intake.getCaffeineSnapshot(),
                intake.getSugarSnapshot(),
                intake.getEspressoShotCount(),
                intake.getSugarCubeCount(),
                intake.getCaloriesSnapshot(),
                intake.getSodiumSnapshot(),
                intake.getProteinSnapshot(),
                intake.getFatSnapshot(),
                intake.getGoalCaffeineTargetSnapshot(),
                intake.getGoalSugarTargetSnapshot(),
                optionResponses,
                intake.getCreatedAt()
        );
    }

    // ── 영양 스냅샷 계산 ──

    private record NutritionSnapshots(
            int caffeine, int sugar,
            Integer calories, Integer sodium, Integer protein, Integer fat
    ) {
    }

    private NutritionSnapshots calculateNutritionSnapshots(Nutrition nutrition, int quantity) {
        return new NutritionSnapshots(
                multiplyOrZero(nutrition.getCaffeineMg(), quantity),
                multiplyOrZero(nutrition.getSugarG(), quantity),
                multiplyNullable(nutrition.getCalories(), quantity),
                multiplyNullable(nutrition.getSodiumMg(), quantity),
                multiplyNullable(nutrition.getProteinG(), quantity),
                multiplyNullable(nutrition.getFatG(), quantity)
        );
    }

    // ── 유틸 ──

    private LocalDate resolveIntakeDate(LocalDate intakeDate) {
        return intakeDate != null ? intakeDate : LocalDate.now();
    }

    private int resolveQuantity(Integer quantity) {
        return quantity != null ? quantity : 1;
    }

    private int multiplyOrZero(Integer value, int multiplier) {
        return value != null ? value * multiplier : 0;
    }

    private Integer multiplyNullable(Integer value, int multiplier) {
        return value != null ? value * multiplier : null;
    }
}
