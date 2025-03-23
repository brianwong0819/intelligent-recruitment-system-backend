package com.event.recruitment.intelligent_recruitment_system.dto;

import com.event.recruitment.intelligent_recruitment_system.model.Gender;
import com.event.recruitment.intelligent_recruitment_system.model.Race;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

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
    private LocalDate dateOfBirth;  // Should ideally be in YYYY-MM-DD format

    @NotNull(message = "Race is required")
    private Race race;

    private String profilePictureUrl;  // Can be updated later
}
