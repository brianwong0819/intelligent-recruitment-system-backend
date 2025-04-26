package com.event.recruitment.intelligent_recruitment_system.dto.request.job;

import com.event.recruitment.intelligent_recruitment_system.model.enums.JobStatusType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobTitleType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.SalaryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateJobRequest {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotBlank(message = "Job title is required")
    @Size(max = 50, message = "Job title must not exceed 50 characters")
    private String title;

    private JobTitleType jobTitleType;

    @NotBlank(message = "Job scope is required")
    private String jobScope;

    @NotBlank(message = "Job requirements are required")
    private String requirements;

    @NotNull(message = "Salary is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Salary must be greater than 0")
    private BigDecimal salary;

    private String paymentTerms;

    private SalaryType salaryType;

    private String benefits;

    private JobStatusType status;
}