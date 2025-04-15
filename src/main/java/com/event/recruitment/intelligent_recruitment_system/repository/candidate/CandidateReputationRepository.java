// src/main/java/com/event/recruitment/intelligent_recruitment_system/repository/candidate/CandidateReputationRepository.java
package com.event.recruitment.intelligent_recruitment_system.repository.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateReputation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateReputationRepository extends JpaRepository<CandidateReputation, Long> {
    Optional<CandidateReputation> findByCandidateId(Long candidateId);
}