// src/main/java/com/event/recruitment/intelligent_recruitment_system/dto/request/job/UpdateJobRequest.java

package com.event.recruitment.intelligent_recruitment_system.dto.request.job;

import com.event.recruitment.intelligent_recruitment_system.model.enums.JobTitleType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.SalaryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class UpdateJobRequest {
    // No project ID field - we don't allow moving jobs between projects

    @NotBlank(message = "Job title is required")
    @Size(max = 50, message = "Job title must not exceed 50 characters")
    private String title;

    private JobTitleType jobTitleType;

    @NotBlank(message = "Job scope is required")
    private String jobScope;

    @NotBlank(message = "Job requirements are required")
    private String requirements;

    @DecimalMin(value = "0.0", inclusive = false, message = "Salary must be greater than 0")
    private BigDecimal salary;

    private String paymentTerms;

    private SalaryType salaryType;

    private String benefits;

    // Status updates should be handled through the dedicated status change endpoint
}