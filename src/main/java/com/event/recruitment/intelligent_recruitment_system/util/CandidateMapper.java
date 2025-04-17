package com.event.recruitment.intelligent_recruitment_system.util;

import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.*;
import com.event.recruitment.intelligent_recruitment_system.dto.response.location.LocationResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateExperience;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateSelfphotoComcard;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateWorkingPhoto;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Language;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
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
        if (candidate.getLanguages() != null && !candidate.getLanguages().isEmpty()) {
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
        if (candidate.getLanguages() != null && !candidate.getLanguages().isEmpty()) {
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

    /**
     * Convert a Candidate entity to a CandidateSearchResponseDTO
     * @param candidate The candidate entity
     * @param experiences The candidate's experiences
     * @return CandidateSearchResponseDTO
     */
    public CandidateSearchResponseDTO toSearchResponseDTO(Candidates candidate, List<CandidateExperience> experiences) {
        // Calculate age from date of birth if available
        String age = "N/A";
        if (candidate.getDateOfBirth() != null) {
            int ageValue = Period.between(candidate.getDateOfBirth(), LocalDate.now()).getYears();
            age = String.valueOf(ageValue);
        }

        // Extract job types from experiences - convert enum to string
        List<String> experienceTypes = experiences.stream()
                .map(exp -> exp.getJobType() != null ? exp.getJobType().name() : null)
                .filter(jobType -> jobType != null)
                .collect(Collectors.toList());

        // Get preferred location name if available
        String locationName = candidate.getPreferredLocation() != null ?
                candidate.getPreferredLocation().getName() : null;

        // Build the response DTO
        return CandidateSearchResponseDTO.builder()
                .id(candidate.getId())
                .name(candidate.getName())
                .profilePictureUrl(candidate.getProfilePictureUrl())
                .gender(candidate.getGender())
                .age(age)
                .ethnicity(candidate.getRace())
                .languages(candidate.getLanguages())
                .employmentStatus(candidate.getEmploymentStatus())
                .availability(candidate.getAvailability())
                .preferredLocationName(locationName)
                .experienceCount(experiences.size())
                .experienceTypes(experienceTypes)
                .bio(candidate.getBio())
                .build();
    }
}