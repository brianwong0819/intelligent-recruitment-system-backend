package com.event.recruitment.intelligent_recruitment_system.repository.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Availability;
import com.event.recruitment.intelligent_recruitment_system.model.enums.EmploymentStatus;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Gender;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Race;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidates, Long>, JpaSpecificationExecutor<Candidates> {

    Optional<Candidates> findByEmail(String email);

    Optional<Candidates> findByUsername(String username);

    Optional<Candidates> findByPhoneNumber(String phoneNumber);

    Optional<Candidates> findByOauthId(String oauthId);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    // Method to find all non-deleted candidates with pagination
    Page<Candidates> findByIsDeletedFalse(Pageable pageable);

    // Find searchable candidates
    Page<Candidates> findByIsSearchableTrueAndIsDeletedFalse(Pageable pageable);

    // Filtered queries
    @Query("SELECT c FROM Candidates c WHERE c.isSearchable = true AND c.isDeleted = false " +
            "AND (:availability IS NULL OR c.availability = :availability)")
    Page<Candidates> findSearchableByAvailability(
            @Param("availability") Availability availability,
            Pageable pageable);

    @Query("SELECT c FROM Candidates c WHERE c.isSearchable = true AND c.isDeleted = false " +
            "AND (:employmentStatus IS NULL OR c.employmentStatus = :employmentStatus)")
    Page<Candidates> findSearchableByEmploymentStatus(
            @Param("employmentStatus") EmploymentStatus employmentStatus,
            Pageable pageable);

    @Query("SELECT c FROM Candidates c WHERE c.isSearchable = true AND c.isDeleted = false " +
            "AND (:gender IS NULL OR c.gender = :gender)")
    Page<Candidates> findSearchableByGender(
            @Param("gender") Gender gender,
            Pageable pageable);

    @Query("SELECT c FROM Candidates c WHERE c.isSearchable = true AND c.isDeleted = false " +
            "AND (:ethnicity IS NULL OR c.race = :ethnicity)")
    Page<Candidates> findSearchableByEthnicity(
            @Param("ethnicity") Race ethnicity,
            Pageable pageable);

    // Complex query with multiple filters
    @Query("SELECT c FROM Candidates c " +
            "WHERE c.isSearchable = true AND c.isDeleted = false " +
            "AND (:availability IS NULL OR c.availability = :availability) " +
            "AND (:employmentStatus IS NULL OR c.employmentStatus = :employmentStatus) " +
            "AND (:gender IS NULL OR c.gender = :gender) " +
            "AND (:ethnicity IS NULL OR c.race = :ethnicity) " +
            "AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(c.bio) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Candidates> searchCandidates(
            @Param("availability") Availability availability,
            @Param("employmentStatus") EmploymentStatus employmentStatus,
            @Param("gender") Gender gender,
            @Param("ethnicity") Race ethnicity,
            @Param("keyword") String keyword,
            Pageable pageable);
}