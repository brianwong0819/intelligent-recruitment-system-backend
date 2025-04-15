// src/main/java/com/event/recruitment/intelligent_recruitment_system/repository/ai/AIRatingRepository.java
package com.event.recruitment.intelligent_recruitment_system.repository.ai;

import com.event.recruitment.intelligent_recruitment_system.model.entity.ai.AIRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AIRatingRepository extends JpaRepository<AIRating, Long> {
    Optional<AIRating> findByJobApplicationId(Long jobApplicationId);
}