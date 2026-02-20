package com.lastcup.api.domain.intake.repository;

import com.lastcup.api.domain.intake.domain.Intake;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class IntakeRepositoryTest {

    @Autowired private IntakeRepository intakeRepository;
    @Autowired private EntityManager em;

    @Test
    @DisplayName("findDistinctIntakeDates: 중복 날짜를 제거하고 정렬해서 반환한다")
    void findDistinctIntakeDatesReturnsDistinctDates() {
        Intake i1 = Intake.create(1L, LocalDate.of(2026, 1, 10), 11L, 1, 100, 5, null, null, null, null, 400, 25);
        Intake i2 = Intake.create(1L, LocalDate.of(2026, 1, 10), 12L, 1, 120, 6, null, null, null, null, 400, 25);
        Intake i3 = Intake.create(1L, LocalDate.of(2026, 1, 12), 13L, 1, 130, 7, null, null, null, null, 400, 25);
        em.persist(i1);
        em.persist(i2);
        em.persist(i3);
        em.flush();

        List<LocalDate> dates = intakeRepository.findDistinctIntakeDates(
                1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)
        );

        assertEquals(List.of(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12)), dates);
    }
}
