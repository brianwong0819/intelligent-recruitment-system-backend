// src/main/java/com/event/recruitment/intelligent_recruitment_system/dto/response/job/JobResponseDTO.java

package com.event.recruitment.intelligent_recruitment_system.dto.response.job;

import com.event.recruitment.intelligent_recruitment_system.model.enums.JobStatusType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobTitleType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.RecruiterType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.SalaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponseDTO {
    private Long id;
    private Long projectId;
    private String projectName;
    private String title;
    private JobTitleType jobTitleType;
    private String jobScope;
    private String requirements;
    private BigDecimal salary;
    private String paymentTerms;
    private SalaryType salaryType;
    private String benefits;
    private JobStatusType status;
    private LocalDateTime createdAt;

    // Company/recruiter information
    private Long recruiterId;
    private String companyName;
    private String companyLogoUrl;
    private RecruiterType recruiterType;

    // Job schedules information
    private List<JobScheduleResponseDTO> jobSchedules;
}