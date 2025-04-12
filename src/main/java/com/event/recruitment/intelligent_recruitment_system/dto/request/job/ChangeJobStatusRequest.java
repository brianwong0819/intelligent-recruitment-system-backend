package com.event.recruitment.intelligent_recruitment_system.dto.request.job;

import com.event.recruitment.intelligent_recruitment_system.model.enums.JobStatusType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeJobStatusRequest {
    private Long jobId; // Made optional

    @NotNull(message = "New status is required")
    private JobStatusType newStatus;
}