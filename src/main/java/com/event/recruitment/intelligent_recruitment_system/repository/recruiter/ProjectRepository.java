package com.event.recruitment.intelligent_recruitment_system.repository.recruiter;

import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Projects;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Recruiters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Projects, Long> {
    /**
     * Find all projects for a recruiter by recruiter ID
     *
     * @param recruiterId The recruiter ID
     * @return List of projects belonging to the recruiter
     */
    List<Projects> findByRecruiterId(Long recruiterId);

    /**
     * Find all projects for a recruiter
     *
     * @param recruiter The recruiter entity
     * @return List of projects belonging to the recruiter
     */
    List<Projects> findByRecruiter(Recruiters recruiter);

    /**
     * Find a project by ID and recruiter ID
     * This ensures recruiters can only access their own projects
     *
     * @param id The project ID
     * @param recruiterId The recruiter ID
     * @return Optional project if found
     */
    Optional<Projects> findByIdAndRecruiterId(Long id, Long recruiterId);

    /**
     * Check if all jobs in the project are cancelled
     * Returns true if there are no jobs or all jobs are cancelled
     *
     * @param projectId The project ID
     * @return true if all jobs are cancelled, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(j) = 0 OR " +
            "SUM(CASE WHEN j.status = 'CANCELLED' THEN 1 ELSE 0 END) = COUNT(j) " +
            "THEN true ELSE false END " +
            "FROM Jobs j WHERE j.project.id = :projectId")
    boolean areAllJobsCancelled(@Param("projectId") Long projectId);

    /**
     * Find a project by ID that is not marked as deleted
     *
     * @param id The project ID
     * @return Optional project if found and not deleted
     */
    Optional<Projects> findByIdAndIsDeletedFalse(Long id);

    /**
     * Find all projects for a recruiter that are not deleted
     *
     * @param recruiter The recruiter entity
     * @return List of active projects for the recruiter
     */
    List<Projects> findByRecruiterAndIsDeletedFalse(Recruiters recruiter);

    /**
     * Count the number of projects for a recruiter
     *
     * @param recruiterId The recruiter ID
     * @return Count of projects
     */
    long countByRecruiterId(Long recruiterId);

    /**
     * Count the number of active projects for a recruiter
     *
     * @param recruiterId The recruiter ID
     * @return Count of active projects
     */
    long countByRecruiterIdAndIsDeletedFalse(Long recruiterId);
}