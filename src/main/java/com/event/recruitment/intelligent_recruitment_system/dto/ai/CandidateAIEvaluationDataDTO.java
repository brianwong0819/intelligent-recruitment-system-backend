package com.event.recruitment.intelligent_recruitment_system.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateAIEvaluationDataDTO {

    // Job Application info
    private String applicationGroupId;
    private List<Long> jobApplicationIds;

    // Candidate personal info
    private Long candidateId;
    private String candidateName;
    private LocalDate dateOfBirth;
    private String ethnicity;
    private String gender;
    private String bio;
    private String employmentStatus;
    private List<String> languages;

    // Resume
    private String resumeUrl;

    // Candidate availability
    private String availabilityType;
    private List<String> availableDates;


    // Work experience
    private List<ExperienceData> experiences;

    // Job details
    private Long jobId;
    private String jobTitle;
    private String jobTitleType;
    private String jobScope;
    private String jobRequirements;
    private String salaryType;

    // Job location and schedule info
    private List<String> locationNames;
    private LocalDateTime applicationDate;
    private Double distanceToCandidate;
    private Integer totalJobWorkingDays;

    // Work dates info
    private List<String> appliedWorkDates;
    private Integer totalWorkDays;

    private Double reputationScore;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperienceData {
        private String jobType;
        private String experienceText;
    }
}