package com.event.recruitment.intelligent_recruitment_system.repository;

import com.event.recruitment.intelligent_recruitment_system.model.Candidates;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidates, Long> {

    Optional<Candidates> findByEmail(String email);

    Optional<Candidates> findByUsername(String username);

    Optional<Candidates> findByPhoneNumber(String phoneNumber);

    Optional<Candidates> findByOauthId(String oauthId);
}
