package com.event.recruitment.intelligent_recruitment_system.dto.response.candidate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDetailedResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String gender;
    private String dateOfBirth;
    private String race;
    private String profilePictureUrl;
    private String availability;
    private String bio;
    private String resumeUrl;
    private String employmentStatus;
    private String preferredLocation;
    private Set<String> languages;
    private List<CandidateExperienceDTO> experiences;
    private List<CandidateWorkingPhotoDTO> workingPhotos;
    private List<CandidateComcardDTO> comcards;
    private List<String> availabilityDates;
}