package com.event.recruitment.intelligent_recruitment_system.dto.request.auth;

import com.event.recruitment.intelligent_recruitment_system.model.enums.RecruiterType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.VerificationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecruiterRegistrationRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Recruiter's representative name is required")
    private String recruiterRepName;

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotNull(message = "Recruiter type is required")
    private RecruiterType recruiterType;  // Enum type for recruiter type (e.g., Individual, Agency)

    // Company details
    private String companyName;
    private String companyLogoUrl;
    private String companyDescription;
    private String companyLocation;
    private String companyWebsite;

    // Verification & Account Status
    private VerificationStatus verificationStatus;  // Enum type for verification status (Pending, Verified, Banned)
}
