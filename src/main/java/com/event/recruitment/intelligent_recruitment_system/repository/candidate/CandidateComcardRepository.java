package com.event.recruitment.intelligent_recruitment_system.repository.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateSelfphotoComcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateComcardRepository extends JpaRepository<CandidateSelfphotoComcard, Long> {
    Optional<CandidateSelfphotoComcard> findById(Long id);
    Optional<CandidateSelfphotoComcard> findByCandidateId(Long candidateId);
    long countByCandidateId(Long candidateId);
    List<CandidateSelfphotoComcard> findAllByCandidateId(Long candidateId);
}