package com.event.recruitment.intelligent_recruitment_system.util;

import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateComcardDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateExperienceDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateProfileDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateSummaryDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateWorkingPhotoDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.location.LocationResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateExperience;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateSelfphotoComcard;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateWorkingPhoto;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CandidateMapper {

    // src/main/java/com/event/recruitment/intelligent_recruitment_system/util/CandidateMapper.java

    public static CandidateResponseDTO toCandidateResponseDTO(Candidates candidate) {
        // Create LocationResponseDTO from Location entity if it exists
        LocationResponseDTO locationDTO = null;
        if (candidate.getPreferredLocation() != null) {
            locationDTO = LocationResponseDTO.builder()
                    .id(candidate.getPreferredLocation().getId())
                    .name(candidate.getPreferredLocation().getName())
                    .address(candidate.getPreferredLocation().getAddress())
                    .city(candidate.getPreferredLocation().getCity())
                    .state(candidate.getPreferredLocation().getState())
                    .country(candidate.getPreferredLocation().getCountry())
                    .postalCode(candidate.getPreferredLocation().getPostalCode())
                    .latitude(candidate.getPreferredLocation().getLatitude())
                    .longitude(candidate.getPreferredLocation().getLongitude())
                    .placeId(candidate.getPreferredLocation().getPlaceId())
                    .googleMapsUrl(candidate.getPreferredLocation().getGoogleMapsUrl())
                    .build();
        }

        return CandidateResponseDTO.builder()
                .id(candidate.getId())
                .username(candidate.getUsername())
                .name(candidate.getName())
                .email(candidate.getEmail())
                .phoneNumber(candidate.getPhoneNumber())
                .gender(candidate.getGender() != null ? candidate.getGender().toString() : null)
                .dateOfBirth(candidate.getDateOfBirth() != null ? candidate.getDateOfBirth().toString() : null)
                .race(candidate.getRace() != null ? candidate.getRace().toString() : null)
                .profilePictureUrl(candidate.getProfilePictureUrl())
                .preferredLocation(locationDTO)  // Use location DTO instead of string
                .availability(candidate.getAvailability()!= null ? candidate.getAvailability().toString() : null)
                .bio(candidate.getBio())
                .resumeUrl(candidate.getResumeUrl())
                .build();
    }

    /**
     * Maps a Candidate entity to a CandidateProfileDTO with basic information
     * (other collections like experiences, photos, etc. need to be set separately)
     */
    public CandidateProfileDTO toCandidateProfileDTO(Candidates candidate) {
        if (candidate == null) {
            return null;
        }

        // Map location to string representation for the profile DTO
        String locationString = null;
        if (candidate.getPreferredLocation() != null) {
            locationString = candidate.getPreferredLocation().getName();
        }

        CandidateProfileDTO profileDTO = new CandidateProfileDTO();
        profileDTO.setId(candidate.getId());
        profileDTO.setUsername(candidate.getUsername());
        profileDTO.setName(candidate.getName());
        profileDTO.setEmail(candidate.getEmail());
        profileDTO.setPhoneNumber(candidate.getPhoneNumber());
        profileDTO.setGender(candidate.getGender());
        profileDTO.setDateOfBirth(candidate.getDateOfBirth() != null ? candidate.getDateOfBirth().toString() : null);
        profileDTO.setRace(candidate.getRace());
        profileDTO.setProfilePictureUrl(candidate.getProfilePictureUrl());
        profileDTO.setAvailability(candidate.getAvailability());
        profileDTO.setBio(candidate.getBio());
        profileDTO.setResumeUrl(candidate.getResumeUrl());
        profileDTO.setEmploymentStatus(candidate.getEmploymentStatus() != null ?
                candidate.getEmploymentStatus().name() : null);
        profileDTO.setPreferredLocation(locationString);

        // Convert languages enum list to string list
        if (candidate.getLanguages() != null) {
            profileDTO.setLanguages(
                    candidate.getLanguages().stream()
                            .map(Enum::name)
                            .collect(Collectors.toList())
            );
        } else {
            profileDTO.setLanguages(new ArrayList<>());
        }

        return profileDTO;
    }

    /**
     * Maps a Candidate entity to a CandidateSummaryDTO for list views
     */
    public CandidateSummaryDTO toCandidateSummaryDTO(Candidates candidate) {
        if (candidate == null) {
            return null;
        }

        // Map location to string representation for the summary DTO
        String locationString = null;
        if (candidate.getPreferredLocation() != null) {
            locationString = candidate.getPreferredLocation().getName();
        }

        // Convert languages enum list to string list
        List<String> languageStrings = new ArrayList<>();
        if (candidate.getLanguages() != null) {
            languageStrings = candidate.getLanguages().stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());
        }

        CandidateSummaryDTO summaryDTO = new CandidateSummaryDTO();
        summaryDTO.setId(candidate.getId());
        summaryDTO.setName(candidate.getName());
        summaryDTO.setProfilePictureUrl(candidate.getProfilePictureUrl());
        summaryDTO.setGender(candidate.getGender());
        summaryDTO.setAvailability(candidate.getAvailability());
        summaryDTO.setPreferredLocation(locationString);
        summaryDTO.setLanguages(languageStrings);

        return summaryDTO;
    }

    /**
     * Maps a CandidateExperience entity to a CandidateExperienceDTO
     */
    public CandidateExperienceDTO toExperienceDTO(CandidateExperience experience) {
        if (experience == null) {
            return null;
        }

        return new CandidateExperienceDTO(experience);
    }

    /**
     * Maps a CandidateWorkingPhoto entity to a CandidateWorkingPhotoDTO
     */
    public CandidateWorkingPhotoDTO toWorkingPhotoDTO(CandidateWorkingPhoto photo) {
        if (photo == null) {
            return null;
        }

        return new CandidateWorkingPhotoDTO(photo);
    }

    /**
     * Maps a CandidateSelfphotoComcard entity to a CandidateComcardDTO
     */
    public CandidateComcardDTO toComcardDTO(CandidateSelfphotoComcard comcard) {
        if (comcard == null) {
            return null;
        }

        return new CandidateComcardDTO(comcard);
    }
}