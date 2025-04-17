package com.event.recruitment.intelligent_recruitment_system.dto.response.admin;

import com.event.recruitment.intelligent_recruitment_system.model.enums.RecruiterType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecruiterSummaryDTO {
    private Long id;
    private String username;
    private String recruiterRepName;
    private String email;
    private String phoneNumber;
    private RecruiterType recruiterType;
    private String companyName;
    private String companyLogoUrl;
    private VerificationStatus verificationStatus;
    private LocalDateTime createdAt;
}