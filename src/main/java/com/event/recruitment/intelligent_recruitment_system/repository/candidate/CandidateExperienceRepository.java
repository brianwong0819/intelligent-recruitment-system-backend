package com.event.recruitment.intelligent_recruitment_system.repository.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateExperienceRepository extends JpaRepository<CandidateExperience, Long> {
    List<CandidateExperience> findByCandidateId(Long candidateId);
    Optional<CandidateExperience> findByIdAndCandidateId(Long id, Long candidateId);
    long countByCandidateId(Long candidateId);
    void deleteByIdAndCandidateId(Long id, Long candidateId);
}