// src/main/java/com/event/recruitment/intelligent_recruitment_system/dto/response/job/JobSummaryResponseDTO.java

package com.event.recruitment.intelligent_recruitment_system.dto.response.job;

import com.event.recruitment.intelligent_recruitment_system.model.enums.JobTitleType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.RecruiterType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.SalaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSummaryResponseDTO {
    private Long id;
    private String title;
    private JobTitleType jobTitleType;
    private Long recruiterId;
    private String companyName;
    private String companyLogoUrl;
    private RecruiterType recruiterType;
    private List<String> locations;
    private BigDecimal salary;
    private SalaryType salaryType;
    private String paymentTerms;
    private String benefits;
    private String jobScope;
    private String jobRequirements;
    private LocalDateTime createdAt;
    private LocalDate earliestStartDate;
    private LocalDate latestEndDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer totalPositions;
    private Integer availablePositions;
    private Double distance; // In kilometers, if distance-based search was used
    private Integer totalWorkDays;


    private boolean saved;
    private boolean viewed;
}