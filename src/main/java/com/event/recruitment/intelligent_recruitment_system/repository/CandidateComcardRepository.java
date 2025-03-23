package com.event.recruitment.intelligent_recruitment_system.repository;

import com.event.recruitment.intelligent_recruitment_system.model.CandidateSelfphotoComcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateComcardRepository extends JpaRepository<CandidateSelfphotoComcard, Long> {
    Optional<CandidateSelfphotoComcard> findByCandidateId(Long candidateId);
    void deleteByCandidateId(Long candidateId);
}