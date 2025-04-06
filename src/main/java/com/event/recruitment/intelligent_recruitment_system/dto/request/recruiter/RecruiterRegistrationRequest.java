package com.event.recruitment.intelligent_recruitment_system.dto.request.recruiter;

import com.event.recruitment.intelligent_recruitment_system.model.enums.RecruiterType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecruiterRegistrationRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Representative name is required")
    @Size(min = 2, max = 255, message = "Representative name must be between 2 and 255 characters")
    private String recruiterRepName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Phone number must be valid")
    private String phoneNumber;

    private RecruiterType recruiterType;

    private String companyName;

    private String companyDescription;

    private String companyLocation;

    private String companyWebsite;
}