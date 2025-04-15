package com.event.recruitment.intelligent_recruitment_system.dto.response.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.enums.Availability;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Gender;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Race;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfileDTO {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String phoneNumber;
    private Gender gender;
    private String dateOfBirth;
    private Race race;
    private String profilePictureUrl;
    private Availability availability;
    private String bio;
    private String resumeUrl;
    private String employmentStatus;
    private String preferredLocation;

    // Nested collections
    private List<String> languages = new ArrayList<>();
    private List<CandidateExperienceDTO> experiences = new ArrayList<>();
    private List<CandidateWorkingPhotoDTO> workingPhotos = new ArrayList<>();
    private List<CandidateComcardDTO> comcards = new ArrayList<>();
    private List<String> availabilityDates = new ArrayList<>();

    // Optional - if you want to include reputation data
    private Double reputationScore;
}