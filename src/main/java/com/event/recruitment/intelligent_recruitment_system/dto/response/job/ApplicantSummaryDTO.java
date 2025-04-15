package com.event.recruitment.intelligent_recruitment_system.dto.response.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantSummaryDTO {
    private Long id;
    private Long candidateId;
    private String candidateName;
    private String email;
    private String phoneNumber;
    private String profilePictureUrl;
    private String gender;
    private String applicationStatus;
    private LocalDateTime applicationDate;

    // Basic list of location names for backward compatibility
    private List<String> locationNames;

    // Updated to support multiple dates per location
    private Map<String, List<LocalDateTime>> locationWorkDates;

    // AI ratings
    private BigDecimal finalScore;
    private BigDecimal experienceScore;
    private BigDecimal skillsScore;
    private BigDecimal locationScore;
    private BigDecimal availabilityScore;
    private BigDecimal resumeScore;
    private BigDecimal reputationScore;

    // AI feedback
    private String aiFeedback;

    // Distance from candidate to job
    private Double distanceToJob;

    // Group ID for linked applications
    private String applicationGroupId;

    private String notes;

    // Reason for withdrawal (if applicable)
    private String withdrawalReason;
}