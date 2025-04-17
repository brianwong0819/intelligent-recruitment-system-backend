package com.event.recruitment.intelligent_recruitment_system.dto.request.admin;

import com.event.recruitment.intelligent_recruitment_system.model.enums.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRecruiterVerificationRequest {
    @NotNull
    private Long recruiterId;

    @NotNull
    private VerificationStatus verificationStatus;
}