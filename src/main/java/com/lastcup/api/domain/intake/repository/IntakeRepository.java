package com.lastcup.api.domain.intake.repository;

import com.lastcup.api.domain.intake.domain.Intake;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntakeRepository extends JpaRepository<Intake, Long> {

    @EntityGraph(attributePaths = {"intakeOptions"})
    @Query("""
            select distinct i from Intake i
            where i.userId = :userId and i.intakeDate = :date
            order by i.createdAt desc
            """)
    List<Intake> findDailyIntakes(@Param("userId") Long userId, @Param("date") LocalDate date);

    @EntityGraph(attributePaths = {"intakeOptions"})
    @Query("""
            select distinct i from Intake i
            where i.userId = :userId
              and i.intakeDate between :startDate and :endDate
            order by i.intakeDate desc, i.createdAt desc
            """)
    List<Intake> findPeriodIntakes(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @EntityGraph(attributePaths = {"intakeOptions"})
    Optional<Intake> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select distinct i.intakeDate from Intake i
            where i.userId = :userId
              and i.intakeDate between :startDate and :endDate
            order by i.intakeDate
            """)
    List<LocalDate> findDistinctIntakeDates(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
