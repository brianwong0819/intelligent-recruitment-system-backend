package com.event.recruitment.intelligent_recruitment_system.repository.job;

import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobApplication;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    // Find applications by job location ID
    List<JobApplication> findByJobLocationId(Long jobLocationId);

    Optional<JobApplication> findByJobLocationAndCandidate(JobLocation jobLocation, Candidates candidate);

    List<JobApplication> findByCandidateIdOrderByApplicationDateDesc(Long candidateId);

    List<JobApplication> findByApplicationGroupId(String applicationGroupId);

    // Add to JobApplicationRepository
    boolean existsByJobLocationIdAndCandidateId(Long jobLocationId, Long candidateId);

    List<JobApplication> findByJobLocationJobId(Long jobId);

    List<JobApplication> findByApplicationGroupIdAndCandidateId(String applicationGroupId, Long candidateId);

    @Query("SELECT COUNT(ja) FROM JobApplication ja WHERE ja.jobLocation.id = :jobLocationId AND ja.applicationStatus = 'HIRED'")
    int countHiredCandidatesForJobLocation(@Param("jobLocationId") Long jobLocationId);

    @Query("SELECT ja FROM JobApplication ja JOIN FETCH ja.jobLocation jl JOIN FETCH jl.location WHERE ja.id = :id")
    JobApplication findApplicationWithLocationById(@Param("id") Long id);

    @Query("SELECT ja FROM JobApplication ja " +
            "JOIN FETCH ja.jobLocation jl " +
            "JOIN FETCH jl.job j " +
            "JOIN FETCH j.project p " +
            "JOIN FETCH p.recruiter r " +
            "JOIN FETCH jl.location loc " +
            "WHERE ja.id = :applicationId")
    Optional<JobApplication> findByIdWithJobDetails(@Param("applicationId") Long applicationId);

    // Get all applications for a specific job
    @Query("SELECT ja FROM JobApplication ja " +
            "JOIN FETCH ja.candidate c " +
            "JOIN FETCH ja.jobLocation jl " +
            "JOIN FETCH jl.location l " +
            "JOIN FETCH jl.job j " +
            "WHERE jl.job.id = ?1")
    List<JobApplication> findByJobId(Long jobId);

    // Paginated query for applications by job ID
    @Query("SELECT ja FROM JobApplication ja " +
            "JOIN FETCH ja.candidate c " +
            "JOIN FETCH ja.jobLocation jl " +
            "JOIN FETCH jl.location l " +
            "WHERE jl.job.id = ?1")
    Page<JobApplication> findByJobLocationJobId(Long jobId, Pageable pageable);

    // Paginated query for applications by job ID and status
    @Query("SELECT ja FROM JobApplication ja " +
            "JOIN FETCH ja.candidate c " +
            "JOIN FETCH ja.jobLocation jl " +
            "JOIN FETCH jl.location l " +
            "WHERE jl.job.id = ?1 AND ja.applicationStatus = ?2")
    Page<JobApplication> findByJobLocationJobIdAndApplicationStatus(Long jobId, JobApplication.ApplicationStatus status, Pageable pageable);

    // Count unique candidates who applied to a job
    @Query("SELECT COUNT(DISTINCT ja.candidate.id) FROM JobApplication ja " +
            "JOIN ja.jobLocation jl " +
            "WHERE jl.job.id = ?1")
    Long countDistinctCandidatesByJobId(Long jobId);

    // Get applications by status and job ID
    @Query("SELECT ja FROM JobApplication ja " +
            "JOIN ja.jobLocation jl " +
            "WHERE jl.job.id = ?1 AND ja.applicationStatus = ?2")
    List<JobApplication> findByJobIdAndStatus(Long jobId, JobApplication.ApplicationStatus status);

    // Count applications by status and job ID
    @Query("SELECT COUNT(DISTINCT ja.candidate.id) FROM JobApplication ja " +
            "JOIN ja.jobLocation jl " +
            "WHERE jl.job.id = ?1 AND ja.applicationStatus = ?2")
    Long countDistinctCandidatesByJobIdAndStatus(Long jobId, JobApplication.ApplicationStatus status);

    /**
     * Find all applications for a job with complete details
     * @param jobId The job ID
     * @return List of job applications with all required details loaded
     */
    @Query("SELECT DISTINCT ja FROM JobApplication ja " +
            "JOIN FETCH ja.candidate c " +
            "JOIN FETCH ja.jobLocation jl " +
            "JOIN FETCH jl.location l " +
            "JOIN FETCH jl.job j " +
            "LEFT JOIN FETCH jl.jobScheduleDate sd " +
            "LEFT JOIN FETCH j.jobSchedules js " +
            "WHERE jl.job.id = :jobId")
    List<JobApplication> findByJobIdWithFullDetails(@Param("jobId") Long jobId);

    /**
     * Find job applications by status with complete details
     * @param jobId The job ID
     * @param status The application status
     * @return List of job applications with all required details loaded
     */
    @Query("SELECT DISTINCT ja FROM JobApplication ja " +
            "JOIN FETCH ja.candidate c " +
            "JOIN FETCH ja.jobLocation jl " +
            "JOIN FETCH jl.location l " +
            "JOIN FETCH jl.job j " +
            "LEFT JOIN FETCH jl.jobScheduleDate sd " +
            "LEFT JOIN FETCH j.jobSchedules js " +
            "WHERE jl.job.id = :jobId AND ja.applicationStatus = :status")
    List<JobApplication> findByJobIdAndStatusWithFullDetails(
            @Param("jobId") Long jobId,
            @Param("status") JobApplication.ApplicationStatus status);

    /**
     * Find applications by group ID with complete details
     * @param groupId The application group ID
     * @return List of job applications with all required details loaded
     */
    @Query("SELECT DISTINCT ja FROM JobApplication ja " +
            "JOIN FETCH ja.candidate c " +
            "JOIN FETCH ja.jobLocation jl " +
            "JOIN FETCH jl.location l " +
            "JOIN FETCH jl.job j " +
            "LEFT JOIN FETCH jl.jobScheduleDate sd " +
            "LEFT JOIN FETCH j.jobSchedules js " +
            "WHERE ja.applicationGroupId = :groupId")
    List<JobApplication> findByApplicationGroupIdWithFullDetails(@Param("groupId") String groupId);

    /**
     * Find application by ID with complete details
     * @param applicationId The application ID
     * @return The job application with all required details loaded
     */
    @Query("SELECT DISTINCT ja FROM JobApplication ja " +
            "JOIN FETCH ja.candidate c " +
            "JOIN FETCH ja.jobLocation jl " +
            "JOIN FETCH jl.location l " +
            "JOIN FETCH jl.job j " +
            "JOIN FETCH j.project p " +
            "JOIN FETCH p.recruiter r " +
            "LEFT JOIN FETCH jl.jobScheduleDate sd " +
            "LEFT JOIN FETCH j.jobSchedules js " +
            "WHERE ja.id = :applicationId")
    Optional<JobApplication> findByIdWithFullDetails(@Param("applicationId") Long applicationId);

    /**
     * Count candidates by job ID and list of application statuses
     * Used for training status summary
     * @param jobId The job ID
     * @param statuses List of application statuses to include
     * @return Count of candidates
     */
    @Query("SELECT COUNT(DISTINCT ja.candidate.id) FROM JobApplication ja " +
            "JOIN ja.jobLocation jl " +
            "WHERE jl.job.id = :jobId AND ja.applicationStatus IN :statuses")
    long countByJobIdAndApplicationStatusIn(@Param("jobId") Long jobId, @Param("statuses") List<String> statuses);

    /**
     * Find all hired applications with work date on the specified target date
     *
     * @param targetDate The date to check for work assignments
     * @return List of hired applications with work on the target date
     */
    @Query("SELECT ja FROM JobApplication ja " +
            "JOIN FETCH ja.candidate c " +
            "JOIN FETCH ja.jobLocation jl " +
            "JOIN FETCH jl.location loc " +
            "JOIN FETCH jl.jobScheduleDate sd " +
            "JOIN FETCH jl.job j " +
            "JOIN FETCH j.project p " +
            "JOIN FETCH p.recruiter r " +
            "JOIN FETCH j.jobSchedules s " +
            "WHERE ja.applicationStatus = 'HIRED' " +
            "AND sd.workDate = :targetDate")
    List<JobApplication> findUpcomingHiredApplications(@Param("targetDate") LocalDate targetDate);
}