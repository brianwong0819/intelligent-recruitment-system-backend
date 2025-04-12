package com.event.recruitment.intelligent_recruitment_system.util;

import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.location.LocationResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;

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
    }}