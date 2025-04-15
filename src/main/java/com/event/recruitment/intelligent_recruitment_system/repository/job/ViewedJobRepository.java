// src/main/java/com/event/recruitment/intelligent_recruitment_system/repository/job/ViewedJobRepository.java

package com.event.recruitment.intelligent_recruitment_system.repository.job;

import com.event.recruitment.intelligent_recruitment_system.model.entity.job.ViewedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ViewedJobRepository extends JpaRepository<ViewedJob, Long> {

    List<ViewedJob> findByCandidateIdOrderByLastViewedAtDesc(Long candidateId);

    Optional<ViewedJob> findByCandidateIdAndJobId(Long candidateId, Long jobId);
    boolean existsByCandidateIdAndJobId(Long candidateId, Long jobId);

    @Query("SELECT COUNT(DISTINCT v.candidateId) FROM ViewedJob v WHERE v.jobId = :jobId")
    Long countUniqueViewersByJobId(@Param("jobId") Long jobId);

    @Query("SELECT v FROM ViewedJob v WHERE v.jobId = :jobId ORDER BY v.lastViewedAt DESC")
    List<ViewedJob> findAllViewsForJob(@Param("jobId") Long jobId);
}