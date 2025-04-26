package com.event.recruitment.intelligent_recruitment_system.dto.request.auth;

import com.event.recruitment.intelligent_recruitment_system.model.enums.EmploymentStatus;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Gender;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Language;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Race;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
public class CandidateRegistrationRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotNull(message = "Gender is required")
    private Gender gender;  // Enum type for gender (e.g., "Male", "Female", "Other")

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @NotNull(message = "Employment status is required")
    private EmploymentStatus employmentStatus;

    @NotNull(message = "Race is required")
    private Race race;

    private String profilePictureUrl;  // Can be updated later

    // Added bio field - optional
    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    // Added languages field - required
    @NotEmpty(message = "At least one language must be selected")
    private List<Language> languages;
}