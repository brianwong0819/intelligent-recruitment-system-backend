// Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/repository/training/TrainingMaterialRepository.java

package com.event.recruitment.intelligent_recruitment_system.repository.training;

import com.event.recruitment.intelligent_recruitment_system.model.entity.training.TrainingMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingMaterialRepository extends JpaRepository<TrainingMaterial, Long> {

    /**
     * Find training material by job ID
     */
    List<TrainingMaterial> findByJobIdAndIsActiveTrue(Long jobId);

    /**
     * Find enabled training materials for a job (visible to candidates)
     */
    List<TrainingMaterial> findByJobIdAndIsActiveTrueAndIsEnabledTrue(Long jobId);

    /**
     * Find specific training material by ID and job ID
     */
    Optional<TrainingMaterial> findByIdAndJobIdAndIsActiveTrue(Long id, Long jobId);

    /**
     * Check if a job already has a training material
     */
    boolean existsByJobIdAndIsActiveTrue(Long jobId);
}
