package com.event.recruitment.intelligent_recruitment_system.repository.job;

import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobApplication;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

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
}