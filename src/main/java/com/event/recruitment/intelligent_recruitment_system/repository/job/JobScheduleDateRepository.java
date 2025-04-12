package com.event.recruitment.intelligent_recruitment_system.repository.job;

import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobScheduleDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JobScheduleDateRepository extends JpaRepository<JobScheduleDate, Long> {
    /**
     * Find schedule dates by job schedule ID
     * @param jobScheduleId Job Schedule ID
     * @return List of schedule dates
     */
    List<JobScheduleDate> findByJobScheduleId(Long jobScheduleId);

    /**
     * Find schedule dates within a specific date range
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of schedule dates
     */
    @Query("SELECT jsd FROM JobScheduleDate jsd WHERE " +
            "jsd.workDate BETWEEN :startDate AND :endDate")
    List<JobScheduleDate> findScheduleDatesInRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}