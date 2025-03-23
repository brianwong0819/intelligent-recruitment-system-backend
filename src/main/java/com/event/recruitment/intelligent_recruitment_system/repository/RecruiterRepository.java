package com.event.recruitment.intelligent_recruitment_system.repository;

import com.event.recruitment.intelligent_recruitment_system.model.Recruiters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecruiterRepository extends JpaRepository<Recruiters, Long> {

    Optional<Recruiters> findByEmail(String email);

    Optional<Recruiters> findByUsername(String username);

    Optional<Recruiters> findByPhoneNumber(String phoneNumber);

    Optional<Recruiters> findByOauthId(String oauthId);
}
