// src/main/java/com/event/recruitment/intelligent_recruitment_system/repository/ai/AIRatingRepository.java
package com.event.recruitment.intelligent_recruitment_system.repository.ai;

import com.event.recruitment.intelligent_recruitment_system.model.entity.ai.AIRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AIRatingRepository extends JpaRepository<AIRating, Long> {
    // Find by job application ID
    Optional<AIRating> findByJobApplicationId(Long jobApplicationId);

    // Find by application group ID directly (if stored in AIRating)
    Optional<AIRating> findByApplicationGroupId(String applicationGroupId);

    @Query("SELECT ar FROM AIRating ar WHERE ar.applicationGroupId = ?1 AND ar.jobApplicationId = (SELECT MIN(ar2.jobApplicationId) FROM AIRating ar2 WHERE ar2.applicationGroupId = ?1)")
    Optional<AIRating> findFirstByApplicationGroupId(String applicationGroupId);

    // Find all ratings with the same application group ID
    List<AIRating> findAllByApplicationGroupId(String applicationGroupId);

    // Original query - find ratings by joining with JobApplication
    @Query("SELECT ar FROM AIRating ar JOIN JobApplication ja ON ar.jobApplicationId = ja.id " +
            "WHERE ja.applicationGroupId = ?1")
    List<AIRating> findByJobApplicationGroupId(String applicationGroupId);

    @Query("SELECT ar FROM AIRating ar JOIN JobApplication ja ON ar.jobApplicationId = ja.id " +
            "JOIN JobLocation jl ON ja.jobLocation.id = jl.id " +
            "WHERE jl.job.id = ?1")
    List<AIRating> findByJobId(Long jobId);

    @Query("SELECT COUNT(ar) FROM AIRating ar JOIN JobApplication ja ON ar.jobApplicationId = ja.id " +
            "JOIN JobLocation jl ON ja.jobLocation.id = jl.id " +
            "WHERE jl.job.id = ?1")
    Long countByJobId(Long jobId);

    @Query("SELECT AVG(ar.finalScore) FROM AIRating ar JOIN JobApplication ja ON ar.jobApplicationId = ja.id " +
            "JOIN JobLocation jl ON ja.jobLocation.id = jl.id " +
            "WHERE jl.job.id = ?1")
    BigDecimal getAverageScoreForJob(Long jobId);
}