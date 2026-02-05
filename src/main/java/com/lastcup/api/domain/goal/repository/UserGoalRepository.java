package com.lastcup.api.domain.goal.repository;

import com.lastcup.api.domain.goal.domain.UserGoal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface UserGoalRepository extends JpaRepository<UserGoal, Long> {
    Optional<UserGoal> findTopByUserIdAndEndDateIsNullOrderByStartDateDesc(Long userId);

    @Query("""
            select g
            from UserGoal g
            where g.userId = :userId
              and g.startDate <= :date
              and (g.endDate is null or g.endDate >= :date)
            order by g.startDate desc
            """)
    List<UserGoal> findActiveByDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            Pageable pageable
    );
}
