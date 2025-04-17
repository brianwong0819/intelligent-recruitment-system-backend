package com.event.recruitment.intelligent_recruitment_system.repository.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateAvailabilityDate;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateExperience;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Availability;
import com.event.recruitment.intelligent_recruitment_system.model.enums.EmploymentStatus;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Gender;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Language;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Race;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CandidateSpecifications {

    // Base specification for searchable candidates only
    public static Specification<Candidates> isSearchable() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("isSearchable")),
                cb.isFalse(root.get("isDeleted"))
        );
    }

    // Filter by availability
    public static Specification<Candidates> hasAvailability(Availability availability) {
        return availability == null ? null :
                (root, query, cb) -> cb.equal(root.get("availability"), availability);
    }

    // Filter by employment status
    public static Specification<Candidates> hasEmploymentStatus(EmploymentStatus status) {
        return status == null ? null :
                (root, query, cb) -> cb.equal(root.get("employmentStatus"), status);
    }

    // Filter by gender
    public static Specification<Candidates> hasGender(Gender gender) {
        return gender == null ? null :
                (root, query, cb) -> cb.equal(root.get("gender"), gender);
    }

    // Filter by ethnicity (race)
    public static Specification<Candidates> hasEthnicity(Race ethnicity) {
        return ethnicity == null ? null :
                (root, query, cb) -> cb.equal(root.get("race"), ethnicity);
    }

    // Filter by age range (calculated from date of birth)
    public static Specification<Candidates> hasAgeRange(Integer minAge, Integer maxAge) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (minAge != null || maxAge != null) {
                // We'll use the actual LocalDate field
                LocalDate now = LocalDate.now();

                if (minAge != null) {
                    // Calculate the latest birth date for the minimum age
                    LocalDate maxBirthDate = now.minusYears(minAge);
                    predicates.add(cb.lessThanOrEqualTo(root.get("dateOfBirth"), maxBirthDate));
                }

                if (maxAge != null) {
                    // Calculate the earliest birth date for the maximum age
                    LocalDate minBirthDate = now.minusYears(maxAge + 1);
                    predicates.add(cb.greaterThan(root.get("dateOfBirth"), minBirthDate));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            }

            return null;
        };
    }

    // Filter by languages (must have at least one from the list)
    public static Specification<Candidates> hasAnyLanguages(List<Language> languages) {
        return languages == null || languages.isEmpty() ? null :
                (root, query, cb) -> {
                    // Join with the languages collection
                    Join<Candidates, Language> languagesJoin = root.join("languages", JoinType.LEFT);
                    return languagesJoin.in(languages);
                };
    }

    // Filter by minimum number of experiences
    public static Specification<Candidates> hasMinimumExperiences(Integer minExperience) {
        return minExperience == null ? null :
                (root, query, cb) -> {
                    // We need to count the experiences for each candidate
                    Subquery<Long> experienceCountSubquery = query.subquery(Long.class);
                    jakarta.persistence.criteria.Root<CandidateExperience> experienceRoot =
                            experienceCountSubquery.from(CandidateExperience.class);

                    experienceCountSubquery.select(cb.count(experienceRoot))
                            .where(cb.equal(experienceRoot.get("candidateId"), root.get("id")));

                    return cb.greaterThanOrEqualTo(experienceCountSubquery, minExperience.longValue());
                };
    }

    // Filter by available dates (must be available on all the specified dates)
    public static Specification<Candidates> isAvailableOnDates(List<LocalDate> dates) {
        return dates == null || dates.isEmpty() ? null :
                (root, query, cb) -> {
                    // For each date, we need to check if the candidate has an availability entry
                    List<Predicate> datePredicates = new ArrayList<>();

                    for (LocalDate date : dates) {
                        Subquery<Long> availabilitySubquery = query.subquery(Long.class);
                        jakarta.persistence.criteria.Root<CandidateAvailabilityDate> availabilityRoot =
                                availabilitySubquery.from(CandidateAvailabilityDate.class);

                        availabilitySubquery.select(cb.count(availabilityRoot))
                                .where(
                                        cb.and(
                                                cb.equal(availabilityRoot.get("candidateId"), root.get("id")),
                                                cb.equal(availabilityRoot.get("availableDate"), date)
                                        )
                                );

                        datePredicates.add(cb.greaterThan(availabilitySubquery, 0L));
                    }

                    // All dates must match (AND)
                    return cb.and(datePredicates.toArray(new Predicate[0]));
                };
    }

    // Keyword search across name and bio
    public static Specification<Candidates> containsKeyword(String keyword) {
        return keyword == null || keyword.trim().isEmpty() ? null :
                (root, query, cb) -> {
                    String wildcardKeyword = "%" + keyword.toLowerCase() + "%";
                    return cb.or(
                            cb.like(cb.lower(root.get("name")), wildcardKeyword),
                            cb.like(cb.lower(root.get("bio")), wildcardKeyword)
                    );
                };
    }
}