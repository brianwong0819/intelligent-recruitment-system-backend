package com.event.recruitment.intelligent_recruitment_system.dto.request.recruiter;

import com.event.recruitment.intelligent_recruitment_system.model.enums.Availability;
import com.event.recruitment.intelligent_recruitment_system.model.enums.EmploymentStatus;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Gender;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Language;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Race;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSearchRequest {
    // Pagination parameters
    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

    // Sorting parameters
    private String sortBy;
    private Boolean sortDesc;

    // Filter parameters
    private Availability availability;
    private Integer minAge;
    private Integer maxAge;
    private EmploymentStatus employmentStatus;
    private Race ethnicity;
    private List<Language> languages;
    private Gender gender;

    // Experience filter
    private Integer minExperience;

    // Date availability filters
    private List<LocalDate> availableDates;

    // Keyword search
    private String keyword;
}