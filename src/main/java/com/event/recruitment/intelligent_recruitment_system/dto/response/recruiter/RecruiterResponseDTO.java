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
public class RecruiterResponseDTO {
    private Long id;
    private String username;
    private String recruiterRepName;
    private String email;
    private String phoneNumber;
    private RecruiterType recruiterType;
    private String companyName;
    private String companyLogoUrl;
    private String companyDescription;
    // Replace string location with LocationResponseDTO
    private LocationResponseDTO companyLocation;
    private String companyWebsite;
    private VerificationStatus verificationStatus;
}