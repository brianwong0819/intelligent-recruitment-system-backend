package com.event.recruitment.intelligent_recruitment_system.repository.job;

import com.event.recruitment.intelligent_recruitment_system.model.entity.job.Jobs;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Projects;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Jobs, Long> {
    // Find jobs by project
    List<Jobs> findByProject(Projects project);

    // Find jobs by project ID
    List<Jobs> findByProjectId(Long projectId);

    // Count total jobs by project ID
    Long countByProjectId(Long projectId);

    // Find jobs by specific status
    List<Jobs> findByStatus(JobStatusType status);

    // Find jobs by project ID and status
    List<Jobs> findByProjectIdAndStatus(Long projectId, JobStatusType status);

    // Count jobs by project ID and status
    Long countByProjectIdAndStatus(Long projectId, JobStatusType status);

    // Find active jobs in a project (not cancelled)
    @Query("SELECT j FROM Jobs j WHERE j.project.id = :projectId AND j.status != 'CANCELLED'")
    List<Jobs> findActiveJobsByProjectId(@Param("projectId") Long projectId);

    // Count active jobs in a project
    @Query("SELECT COUNT(j) FROM Jobs j WHERE j.project.id = :projectId AND j.status != 'CANCELLED'")
    Long countActiveJobsByProjectId(@Param("projectId") Long projectId);

    // Find jobs by multiple statuses
    @Query("SELECT j FROM Jobs j WHERE j.project.id = :projectId AND j.status IN :statuses")
    List<Jobs> findByProjectIdAndStatusIn(
            @Param("projectId") Long projectId,
            @Param("statuses") List<JobStatusType> statuses
    );

    // Custom query to get job count by status for a project
    @Query("SELECT j.status, COUNT(j) FROM Jobs j WHERE j.project.id = :projectId GROUP BY j.status")
    List<Object[]> countJobsByStatusForProject(@Param("projectId") Long projectId);
}