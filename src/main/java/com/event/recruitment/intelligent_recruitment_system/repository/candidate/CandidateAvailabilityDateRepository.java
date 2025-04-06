package com.event.recruitment.intelligent_recruitment_system.repository.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateAvailabilityDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateAvailabilityDateRepository extends JpaRepository<CandidateAvailabilityDate, Long> {

    List<CandidateAvailabilityDate> findByCandidateId(Long candidateId);

    @Modifying
    @Query("DELETE FROM CandidateAvailabilityDate c WHERE c.candidateId = :candidateId")
    void deleteByCandidateId(Long candidateId);
}