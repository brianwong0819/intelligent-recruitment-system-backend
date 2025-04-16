// Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/repository/training/CandidateTrainingRecordRepository.java

package com.event.recruitment.intelligent_recruitment_system.repository.training;

import com.event.recruitment.intelligent_recruitment_system.model.entity.training.CandidateTrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateTrainingRecordRepository extends JpaRepository<CandidateTrainingRecord, Long> {

    /**
     * Find training record for a specific candidate, job, and training material
     */
    Optional<CandidateTrainingRecord> findByCandidateIdAndJobIdAndTrainingMaterialId(
            Long candidateId, Long jobId, Long trainingMaterialId);

    /**
     * Find all training records for a candidate
     */
    List<CandidateTrainingRecord> findByCandidateId(Long candidateId);

    /**
     * Find all training records for a job
     */
    List<CandidateTrainingRecord> findByJobId(Long jobId);

    /**
     * Find all training records for a training material
     */
    List<CandidateTrainingRecord> findByTrainingMaterialId(Long trainingMaterialId);

    /**
     * Find all training records for a job with candidates names
     */
    @Query("SELECT ctr FROM CandidateTrainingRecord ctr " +
            "WHERE ctr.jobId = :jobId")
    List<CandidateTrainingRecord> findByJobIdWithDetails(Long jobId);

    /**
     * Count completed training records for a job
     */
    long countByJobIdAndIsCompletedTrue(Long jobId);

    /**
     * Count total training records for a job
     */
    long countByJobId(Long jobId);
}