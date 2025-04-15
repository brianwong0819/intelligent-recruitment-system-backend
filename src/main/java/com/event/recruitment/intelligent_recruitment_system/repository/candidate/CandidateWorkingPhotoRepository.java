package com.event.recruitment.intelligent_recruitment_system.repository.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateWorkingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateWorkingPhotoRepository extends JpaRepository<CandidateWorkingPhoto, Long> {
    List<CandidateWorkingPhoto> findByCandidateIdOrderByUploadedAtDesc(Long candidateId);

    // Added method for compatibility with service
    default List<CandidateWorkingPhoto> findByCandidateId(Long candidateId) {
        return findByCandidateIdOrderByUploadedAtDesc(candidateId);
    }

    long countByCandidateId(Long candidateId);
    void deleteByCandidateIdAndId(Long candidateId, Long photoId);
}