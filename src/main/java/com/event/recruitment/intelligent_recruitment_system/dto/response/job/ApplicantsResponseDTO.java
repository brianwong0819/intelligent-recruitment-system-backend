package com.event.recruitment.intelligent_recruitment_system.dto.response.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantsResponseDTO {

    private List<ApplicantSummaryDTO> applicants;  // List of applicants
    private long totalApplicants;                  // Total number of applicants
    private int currentPage;                       // Current page
    private int pageSize;                          // Page size
    private int totalPages;                        // Total number of pages
}