package com.event.recruitment.intelligent_recruitment_system.repository.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidates, Long> {

    Optional<Candidates> findByEmail(String email);

    Optional<Candidates> findByUsername(String username);

    Optional<Candidates> findByPhoneNumber(String phoneNumber);

    Optional<Candidates> findByOauthId(String oauthId);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    // Method to find all non-deleted candidates with pagination
    Page<Candidates> findByIsDeletedFalse(Pageable pageable);
}
