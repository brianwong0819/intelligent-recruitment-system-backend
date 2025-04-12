package com.event.recruitment.intelligent_recruitment_system.dto.request.common;

import com.event.recruitment.intelligent_recruitment_system.model.enums.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UpdateProfileRequest {

    // ✅ 适用于 Candidate & Recruiter
    private String name;               // Candidate: name, Recruiter: recruiter_rep_name
    private String email;              // Email for contact
    private String phoneNumber;        // Contact number

    // ✅ 仅适用于 Candidate
    private Gender gender;             // ENUM: Male, Female, Other
    private LocalDate dateOfBirth;     // Date of birth (LocalDate)
    private Race race;                 // ENUM: Malay, Chinese, Indian, Indigenous, Other Bumiputera, Other
    private String profilePictureUrl;  // Profile Picture URL
    private Long preferredLocationId;   // Preferred work location
    private Availability availability; // ENUM: ANYTIME, WEEKDAYS_ONLY, WEEKENDS_ONLY, CUSTOM_DATES
    private String bio;                // Personal introduction
    private List<Language> languages;  // List of languages the candidate speaks
    private String resumeUrl;          // Resume link
    private EmploymentStatus employmentStatus;

    // ✅ 仅适用于 Recruiter
    private RecruiterType recruiterType;  // ENUM: Individual, Freelance, Agency, Company
    private String recruiterRepName;
    private String companyName;           // Company name
    private String companyLogoUrl;        // Company logo URL
    private String companyDescription;    // Company description
    // Replace string location with location ID
    private Long companyLocationId;
    private String companyWebsite;        // Company website
}
