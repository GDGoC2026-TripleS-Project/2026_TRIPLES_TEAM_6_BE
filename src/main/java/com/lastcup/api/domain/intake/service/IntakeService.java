package com.lastcup.api.domain.intake.service;

import com.lastcup.api.domain.goal.domain.UserGoal;
import com.lastcup.api.domain.goal.service.UserGoalService;
import com.lastcup.api.domain.intake.domain.Intake;
import com.lastcup.api.domain.intake.domain.IntakeOption;
import com.lastcup.api.domain.intake.dto.request.CreateIntakeRequest;
import com.lastcup.api.domain.intake.dto.request.IntakeOptionRequest;
import com.lastcup.api.domain.intake.dto.request.IntakeUpdateRequest;
import com.lastcup.api.domain.intake.dto.response.DailyIntakeSummaryResponse;
import com.lastcup.api.domain.intake.dto.response.DrinkGroupResponse;
import com.lastcup.api.domain.intake.dto.response.IntakeDetailResponse;
import com.lastcup.api.domain.intake.dto.response.IntakeOptionDetailResponse;
import com.lastcup.api.domain.intake.dto.response.IntakeOptionResponse;
import com.lastcup.api.domain.intake.dto.response.IntakeRecordDatesResponse;
import com.lastcup.api.domain.intake.dto.response.IntakeResponse;
import com.lastcup.api.domain.intake.dto.response.PeriodIntakeStatisticsResponse;
import com.lastcup.api.domain.intake.repository.IntakeRepository;
import com.lastcup.api.domain.menu.domain.Menu;
import com.lastcup.api.domain.menu.domain.MenuSize;
import com.lastcup.api.domain.menu.domain.MenuTemperature;
import com.lastcup.api.domain.menu.domain.Nutrition;
import com.lastcup.api.domain.menu.repository.MenuSizeRepository;
import com.lastcup.api.domain.option.domain.Option;
import com.lastcup.api.domain.option.repository.OptionRepository;
import static com.lastcup.api.global.config.AppTimeZone.KST;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
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

        // 옵션 검증 및 조회를 한 번에 수행 (성능 개선)
        Map<Long, Option> optionMap = validateAndFetchOptions(request.options());

        int optionCaffeine = calculateOptionCaffeine(request.options(), optionMap, quantity);
        NutritionSnapshots snapshots = calculateNutritionSnapshots(menuSize.getNutrition(), quantity, optionCaffeine);
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

        // 옵션 검증 및 조회를 한 번에 수행 (성능 개선)
        Map<Long, Option> optionMap = validateAndFetchOptions(request.options());

        int optionCaffeine = calculateOptionCaffeine(request.options(), optionMap, quantity);
        NutritionSnapshots snapshots = calculateNutritionSnapshots(menuSize.getNutrition(), quantity, optionCaffeine);
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
        LocalDate targetDate = date != null ? date : LocalDate.now(KST);

        List<Intake> intakes = intakeRepository.findDailyIntakes(userId, targetDate);

        int totalCaffeine = intakes.stream().mapToInt(Intake::getCaffeineSnapshot).sum();
        int totalSugar = intakes.stream().mapToInt(Intake::getSugarSnapshot).sum();
        int intakeCount = intakes.stream().mapToInt(Intake::getQuantity).sum();

        Map<Long, MenuSize> menuSizeMap = fetchMenuSizeMap(intakes);
        Map<Long, Option> optionMap = fetchOptionMap(intakes);
        List<DrinkGroupResponse> drinkGroups = buildDrinkGroups(intakes, menuSizeMap, optionMap);

        Optional<UserGoal> goalOpt = userGoalService.findOptionalByDate(userId, targetDate);
        int goalCaffeine = goalOpt.map(UserGoal::getDailyCaffeineTarget)
                .orElse(UserGoalService.DEFAULT_DAILY_CAFFEINE_TARGET);
        int goalSugar = goalOpt.map(UserGoal::getDailySugarTarget)
                .orElse(UserGoalService.DEFAULT_DAILY_SUGAR_TARGET);

        return new DailyIntakeSummaryResponse(
                targetDate, totalCaffeine, totalSugar,
                Intake.toEspressoShotCount(totalCaffeine),
                Intake.toSugarCubeCount(totalSugar),
                goalCaffeine, goalSugar, intakeCount, drinkGroups
        );
    }

    /**
     * 기간 내 섭취 기록이 존재하는 날짜 목록을 반환한다.
     * 캘린더 UI에서 파란점 표시에 사용되는 경량 API용 메서드.
     */
    @Transactional(readOnly = true)
    public IntakeRecordDatesResponse findIntakeDates(Long userId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }

        List<LocalDate> dates = intakeRepository.findDistinctIntakeDates(userId, startDate, endDate);
        return new IntakeRecordDatesResponse(startDate, endDate, dates);
    }

    /**
     * 기간별 음료 통계 조회: 기간 내 총 섭취량 요약 + 음료 종류별 그룹 통계를 반환한다.
     * 같은 음료라도 ICE/HOT, 사이즈, 옵션 조합이 하나라도 다르면 별도 그룹으로 집계한다.
     */
    @Transactional(readOnly = true)
    public PeriodIntakeStatisticsResponse findPeriodIntakeStatistics(
            Long userId, LocalDate startDate, LocalDate endDate
    ) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }

        List<Intake> intakes = intakeRepository.findPeriodIntakes(userId, startDate, endDate);

        int totalCaffeine = intakes.stream().mapToInt(Intake::getCaffeineSnapshot).sum();
        int totalSugar = intakes.stream().mapToInt(Intake::getSugarSnapshot).sum();

        Map<Long, MenuSize> menuSizeMap = fetchMenuSizeMap(intakes);
        Map<Long, Option> optionMap = fetchOptionMap(intakes);
        List<DrinkGroupResponse> drinkGroups = buildDrinkGroups(intakes, menuSizeMap, optionMap);

        int intakeCount = intakes.stream().mapToInt(Intake::getQuantity).sum();

        return new PeriodIntakeStatisticsResponse(
                startDate, endDate, totalCaffeine, totalSugar,
                Intake.toEspressoShotCount(totalCaffeine),
                Intake.toSugarCubeCount(totalSugar),
                intakeCount, drinkGroups
        );
    }

    @Transactional(readOnly = true)
    public IntakeDetailResponse findIntakeDetail(Long userId, Long intakeId) {
        Intake intake = findIntakeByIdAndUserId(intakeId, userId);

        Map<Long, MenuSize> menuSizeMap = fetchMenuSizeMap(List.of(intake));
        Map<Long, Option> optionMap = fetchOptionMap(List.of(intake));

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

        List<IntakeOptionDetailResponse> options = toOptionDetails(intake, optionMap);

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

    /**
     * 옵션 존재 여부를 검증하고, 영양 정보를 포함하여 조회한다.
     * 검증과 조회를 한 번에 수행하여 중복 쿼리를 방지한다.
     *
     * @param options 검증할 옵션 목록
     * @return 옵션 ID를 키로 하는 Option 맵 (영양 정보 포함)
     * @throws IllegalArgumentException 존재하지 않는 옵션이 있을 경우
     */
    private Map<Long, Option> validateAndFetchOptions(List<IntakeOptionRequest> options) {
        if (options == null || options.isEmpty()) {
            return Map.of();
        }

        Set<Long> optionIds = options.stream()
                .map(IntakeOptionRequest::optionId)
                .collect(Collectors.toSet());

        List<Option> foundOptions = optionRepository.findAllWithNutritionByIdIn(optionIds);

        // 요청한 옵션이 모두 존재하는지 검증
        if (foundOptions.size() != optionIds.size()) {
            Set<Long> foundIds = foundOptions.stream()
                    .map(Option::getId)
                    .collect(Collectors.toSet());
            Set<Long> missingIds = optionIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new IllegalArgumentException("Option not found: " + missingIds);
        }

        return foundOptions.stream()
                .collect(Collectors.toMap(Option::getId, Function.identity()));
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

    /**
     * 섭취 기록 목록에 포함된 모든 옵션을 영양 정보와 함께 일괄 조회한다.
     * 옵션명과 카페인 정보를 응답에 포함하기 위해 nutrition을 페치 조인한다.
     */
    private Map<Long, Option> fetchOptionMap(List<Intake> intakes) {
        Set<Long> optionIds = intakes.stream()
                .flatMap(i -> i.getIntakeOptions().stream())
                .map(IntakeOption::getOptionId)
                .collect(Collectors.toSet());

        if (optionIds.isEmpty()) {
            return Map.of();
        }

        return optionRepository.findAllWithNutritionByIdIn(optionIds).stream()
                .collect(Collectors.toMap(Option::getId, Function.identity()));
    }

    private List<IntakeOptionDetailResponse> toOptionDetails(Intake intake, Map<Long, Option> optionMap) {
        return intake.getIntakeOptions().stream()
                .map(o -> {
                    Option option = optionMap.get(o.getOptionId());
                    String name = option != null ? option.getName() : "";
                    Integer caffeineMg = extractCaffeineMg(option);

                    return new IntakeOptionDetailResponse(
                            o.getOptionId(), name, o.getQuantity(), caffeineMg
                    );
                })
                .toList();
    }

    /**
     * Option에서 카페인 함량을 추출한다.
     * 영양 정보가 없는 옵션(시럽, 토핑 등)은 null을 반환한다.
     */
    private Integer extractCaffeineMg(Option option) {
        if (option == null || option.getNutrition() == null) {
            return null;
        }
        return option.getNutrition().getCaffeineMg();
    }

    // ── 음료 종류별 그룹핑 ──

    /**
     * 음료를 menuSizeId + 옵션 조합(optionId:quantity)으로 그룹핑한다.
     * 같은 음료라도 ICE/HOT, 사이즈, 옵션이 하나라도 다르면 별도 그룹이 된다.
     */
    private List<DrinkGroupResponse> buildDrinkGroups(
            List<Intake> intakes,
            Map<Long, MenuSize> menuSizeMap,
            Map<Long, Option> optionMap
    ) {
        Map<DrinkGroupKey, List<Intake>> grouped = intakes.stream()
                .collect(Collectors.groupingBy(
                        this::toDrinkGroupKey,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.entrySet().stream()
                .map(entry -> toDrinkGroupResponse(entry.getKey(), entry.getValue(), menuSizeMap, optionMap))
                .sorted(Comparator.comparingInt(DrinkGroupResponse::quantity).reversed())
                .toList();
    }

    /**
     * Intake → 그룹핑 키 변환.
     * 옵션 목록은 optionId 기준으로 정렬하여 순서 무관하게 동일한 키를 생성한다.
     */
    private DrinkGroupKey toDrinkGroupKey(Intake intake) {
        List<OptionKey> sortedOptions = intake.getIntakeOptions().stream()
                .map(o -> new OptionKey(o.getOptionId(), o.getQuantity()))
                .sorted()
                .toList();
        return new DrinkGroupKey(intake.getMenuSizeId(), sortedOptions);
    }

    private DrinkGroupResponse toDrinkGroupResponse(
            DrinkGroupKey key,
            List<Intake> groupIntakes,
            Map<Long, MenuSize> menuSizeMap,
            Map<Long, Option> optionMap
    ) {
        int totalQuantity = groupIntakes.stream().mapToInt(Intake::getQuantity).sum();
        int totalCaffeine = groupIntakes.stream().mapToInt(Intake::getCaffeineSnapshot).sum();
        int totalSugar = groupIntakes.stream().mapToInt(Intake::getSugarSnapshot).sum();

        int caffeinePerUnit = totalQuantity > 0
                ? (int) Math.round((double) totalCaffeine / totalQuantity)
                : 0;
        int sugarPerUnit = totalQuantity > 0
                ? (int) Math.round((double) totalSugar / totalQuantity)
                : 0;

        MenuSize menuSize = menuSizeMap.get(key.menuSizeId());
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

        List<IntakeOptionDetailResponse> options = key.options().stream()
                .map(o -> {
                    Option option = optionMap.get(o.optionId());
                    String name = option != null ? option.getName() : "";
                    Integer caffeineMg = extractCaffeineMg(option);

                    return new IntakeOptionDetailResponse(
                            o.optionId(), name, o.quantity(), caffeineMg
                    );
                })
                .toList();

        List<Long> intakeIds = groupIntakes.stream()
                .map(Intake::getId)
                .toList();

        return new DrinkGroupResponse(
                intakeIds,
                brandName, menuName, temperature, sizeName,
                totalCaffeine, totalSugar,
                Intake.toEspressoShotCount(totalCaffeine),
                Intake.toSugarCubeCount(totalSugar),
                totalQuantity, options,
                caffeinePerUnit, sugarPerUnit
        );
    }

    /**
     * 음료 그룹핑 키: menuSizeId + 정렬된 옵션 조합.
     * record이므로 equals/hashCode가 모든 필드 기반으로 자동 생성된다.
     */
    private record DrinkGroupKey(Long menuSizeId, List<OptionKey> options) {
    }

    /**
     * 옵션 키: optionId + quantity 조합.
     * optionId 기준 정렬을 위해 Comparable을 구현한다.
     */
    private record OptionKey(Long optionId, int quantity) implements Comparable<OptionKey> {
        @Override
        public int compareTo(OptionKey other) {
            int cmp = this.optionId.compareTo(other.optionId);
            return cmp != 0 ? cmp : Integer.compare(this.quantity, other.quantity);
        }
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

    /**
     * 옵션의 총 카페인 함량을 계산한다.
     * 
     * 계산 공식: Σ(옵션 카페인 × 옵션 수량) × 음료 수량
     * 
     * 예시: 아메리카노 2잔에 샷 1개(75mg) 추가
     *       → (75 × 1) × 2 = 150mg
     *
     * @param options 옵션 요청 목록
     * @param optionMap 옵션 ID를 키로 하는 Option 맵 (영양 정보 포함)
     * @param drinkQuantity 음료 수량
     * @return 총 옵션 카페인(mg)
     */
    private int calculateOptionCaffeine(
            List<IntakeOptionRequest> options,
            Map<Long, Option> optionMap,
            int drinkQuantity
    ) {
        if (options == null || options.isEmpty()) {
            return 0;
        }

        int totalOptionCaffeine = 0;
        for (IntakeOptionRequest optionReq : options) {
            Option option = optionMap.get(optionReq.optionId());
            
            // 옵션이 존재하지 않으면 이미 validateAndFetchOptions()에서 예외 발생
            // 따라서 여기서는 영양 정보만 체크
            if (option.getNutrition() != null) {
                int caffeineMg = option.getNutrition().getCaffeineMg();
                totalOptionCaffeine += caffeineMg * optionReq.quantity();
            }
            // 영양 정보가 없는 옵션(시럽 등)은 카페인 0mg로 처리
        }

        return totalOptionCaffeine * drinkQuantity;
    }

    private NutritionSnapshots calculateNutritionSnapshots(Nutrition nutrition, int quantity, int optionCaffeine) {
        return new NutritionSnapshots(
                multiplyOrZero(nutrition.getCaffeineMg(), quantity) + optionCaffeine,
                multiplyOrZero(nutrition.getSugarG(), quantity),
                multiplyNullable(nutrition.getCalories(), quantity),
                multiplyNullable(nutrition.getSodiumMg(), quantity),
                multiplyNullable(nutrition.getProteinG(), quantity),
                multiplyNullable(nutrition.getFatG(), quantity)
        );
    }

    // ── 유틸 ──

    private LocalDate resolveIntakeDate(LocalDate intakeDate) {
        return intakeDate != null ? intakeDate : LocalDate.now(KST);
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
