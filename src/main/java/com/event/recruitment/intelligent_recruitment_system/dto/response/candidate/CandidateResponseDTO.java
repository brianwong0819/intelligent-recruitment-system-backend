package com.event.recruitment.intelligent_recruitment_system.dto.response.candidate;

import com.event.recruitment.intelligent_recruitment_system.dto.response.location.LocationResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateResponseDTO {

    private Long id;
    private String username;
    private String email;
    private String name;
    private String phoneNumber;
    private String gender;
    private String dateOfBirth;
    private String race;
    private String profilePictureUrl;
    private LocationResponseDTO preferredLocation;
    private String availability;
    private String bio;
    private String[] languages;
    private String resumeUrl;
    private Boolean isDeleted;
}