package com.event.recruitment.intelligent_recruitment_system.dto.request.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateExperienceRequest {
    @NotNull(message = "Job type is required")
    private JobType jobType;

    @NotBlank(message = "Experience text is required")
    private String experienceText;
}