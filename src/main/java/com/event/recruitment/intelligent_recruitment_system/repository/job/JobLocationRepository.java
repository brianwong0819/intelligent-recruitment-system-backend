package com.event.recruitment.intelligent_recruitment_system.repository.job;

import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobLocation;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobLocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobLocationRepository extends JpaRepository<JobLocation, Long> {

    /**
     * Find job locations by job ID
     * @param jobId Job ID
     * @return List of job locations
     */
    List<JobLocation> findByJobId(Long jobId);


    /**
     * Find job locations by schedule date ID
     * @param scheduleDateId Schedule Date ID
     * @return List of job locations
     */
    List<JobLocation> findByJobScheduleDateId(Long scheduleDateId);

    /**
     * Find job locations by status
     * @param status Job location status
     * @return List of job locations
     */
    List<JobLocation> findByStatus(JobLocationStatus status);

    /**
     * Find job locations with available positions
     * @return List of job locations with open positions
     */
    @Query("SELECT jl FROM JobLocation jl WHERE jl.positionsFilled < jl.positionsNeeded")
    List<JobLocation> findLocationsWithAvailablePositions();

    /**
     * Count available positions for a given job
     * @param jobId Job ID
     * @return Total number of available positions
     */
    @Query("SELECT SUM(jl.positionsNeeded - jl.positionsFilled) FROM JobLocation jl " +
            "WHERE jl.job.id = :jobId")
    Integer countAvailablePositionsForJob(@Param("jobId") Long jobId);

    /**
     * Find job location by ID with job, project, and recruiter details
     * @param id Job location ID
     * @return Optional containing job location with details or empty
     */
    @Query("SELECT jl FROM JobLocation jl " +
            "JOIN FETCH jl.job j " +
            "JOIN FETCH j.project p " +
            "JOIN FETCH p.recruiter r " +
            "JOIN FETCH jl.location loc " +
            "WHERE jl.id = :id")
    Optional<JobLocation> findByIdWithJobDetails(@Param("id") Long id);

    // In JobLocationRepository.java
    @Query("SELECT COUNT(ja) FROM JobApplication ja WHERE ja.jobLocation.id = :locationId")
    long countApplicationsForLocation(@Param("locationId") Long locationId);
}