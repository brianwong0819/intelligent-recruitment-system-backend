package com.event.recruitment.intelligent_recruitment_system.dto.response.candidate;

import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobSummaryResponseDTO;
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
public class JobApplicationResponseDTO {
    private Long id;
    private Long jobId;
    private String applicationGroupId;
    private String jobTitle;
    private String companyName;
    private List<String> locationNames;
    private String applicationStatus;
    private LocalDateTime applicationDate;
    private String notes;
    private List<LocalDate> workDates;
    private List<Long> applicationIds;
    private JobSummaryResponseDTO jobSummary;
    private Double distanceToCandidate; // New field to store distance to candidate's preferred location
}