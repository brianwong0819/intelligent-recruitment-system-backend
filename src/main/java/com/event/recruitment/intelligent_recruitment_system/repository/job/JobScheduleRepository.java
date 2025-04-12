package com.event.recruitment.intelligent_recruitment_system.repository.job;

import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobScheduleRepository extends JpaRepository<JobSchedule, Long> {

    /**
     * Find job schedules for a specific job
     * @param jobId Job ID
     * @return List of job schedules
     */
    List<JobSchedule> findByJobId(Long jobId);

    /**
     * Find job schedules within a specific date range
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of job schedules
     */
    @Query("SELECT js FROM JobSchedule js WHERE " +
            "(js.startDate BETWEEN :startDate AND :endDate) OR " +
            "(js.endDate BETWEEN :startDate AND :endDate) OR " +
            "(:startDate BETWEEN js.startDate AND js.endDate)")
    List<JobSchedule> findSchedulesInDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find a specific job schedule by ID with all associated data
     * @param id Schedule ID
     * @return Optional of JobSchedule with lazy-loaded relations
     */
    @Query("SELECT js FROM JobSchedule js " +
            "LEFT JOIN FETCH js.scheduleDates sd " +
            "WHERE js.id = :id")
    Optional<JobSchedule> findByIdWithDetails(@Param("id") Long id);
}