package com.event.recruitment.intelligent_recruitment_system.repository.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.ReputationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReputationEventRepository extends JpaRepository<ReputationEvent, Long> {
    List<ReputationEvent> findByCandidateIdOrderByCreatedAtDesc(Long candidateId);
    List<ReputationEvent> findByJobApplicationId(Long jobApplicationId);
    boolean existsByEventTypeAndApplicationGroupId(String eventType, String applicationGroupId);
}