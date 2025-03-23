package com.event.recruitment.intelligent_recruitment_system.dto;

import com.event.recruitment.intelligent_recruitment_system.model.*;
import lombok.Data;
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
    private String preferredLocation;  // Preferred work location
    private Availability availability; // ENUM: ANYTIME, WEEKDAYS_ONLY, WEEKENDS_ONLY, CUSTOM_DATES
    private String bio;                // Personal introduction
    private List<Language> languages;  // List of languages the candidate speaks
    private String resumeUrl;          // Resume link

    // ✅ 仅适用于 Recruiter
    private RecruiterType recruiterType;  // ENUM: Individual, Freelance, Agency, Company
    private String companyName;           // Company name
    private String companyLogoUrl;        // Company logo URL
    private String companyDescription;    // Company description
    private String companyLocation;       // Company address/location
    private String companyWebsite;        // Company website
}
