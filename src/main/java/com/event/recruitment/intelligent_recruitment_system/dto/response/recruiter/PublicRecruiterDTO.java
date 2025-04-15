package com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter;

import com.event.recruitment.intelligent_recruitment_system.dto.response.location.LocationResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.enums.RecruiterType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicRecruiterDTO {
    private Long id;
    private String recruiterRepName;
    private RecruiterType recruiterType;
    private String email;
    private String phoneNumber;
    private String companyName;
    private String companyLogoUrl;
    private String companyDescription;
    private LocationResponseDTO companyLocation;
    private String companyWebsite;
    private VerificationStatus verificationStatus;
}