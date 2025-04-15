package com.event.recruitment.intelligent_recruitment_system.dto.request.candidate;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationRequest {
    @NotEmpty(message = "At least one job location ID is required")
    private List<Long> jobLocationIds;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
}