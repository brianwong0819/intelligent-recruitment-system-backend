package com.event.recruitment.intelligent_recruitment_system.dto.response.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSearchResponseDTO {
    private Long id;
    private String name;
    private String profilePictureUrl;
    private Gender gender;
    private String age; // Calculated from DOB
    private Race ethnicity;
    private List<Language> languages;
    private EmploymentStatus employmentStatus;
    private Availability availability;
    private String preferredLocationName; // Only the name of the location
    private Integer experienceCount; // Number of experiences
    private List<String> experienceTypes; // List of job types from experiences
    private String bio;
}