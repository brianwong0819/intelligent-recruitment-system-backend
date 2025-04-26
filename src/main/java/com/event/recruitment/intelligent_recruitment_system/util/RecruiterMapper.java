package com.event.recruitment.intelligent_recruitment_system.util;

import com.event.recruitment.intelligent_recruitment_system.dto.response.location.LocationResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.PublicRecruiterDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.RecruiterResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Recruiters;

public class RecruiterMapper {

    public static RecruiterResponseDTO toRecruiterResponseDTO(Recruiters recruiter) {
        // Create LocationResponseDTO from Location entity if it exists
        LocationResponseDTO locationDTO = null;
        if (recruiter.getCompanyLocation() != null) {
            locationDTO = LocationResponseDTO.builder()
                    .id(recruiter.getCompanyLocation().getId())
                    .name(recruiter.getCompanyLocation().getName())
                    .address(recruiter.getCompanyLocation().getAddress())
                    .city(recruiter.getCompanyLocation().getCity())
                    .state(recruiter.getCompanyLocation().getState())
                    .country(recruiter.getCompanyLocation().getCountry())
                    .postalCode(recruiter.getCompanyLocation().getPostalCode())
                    .latitude(recruiter.getCompanyLocation().getLatitude())
                    .longitude(recruiter.getCompanyLocation().getLongitude())
                    .placeId(recruiter.getCompanyLocation().getPlaceId())
                    .googleMapsUrl(recruiter.getCompanyLocation().getGoogleMapsUrl())
                    .build();
        }

        return RecruiterResponseDTO.builder()
                .id(recruiter.getId())
                .username(recruiter.getUsername())
                .recruiterRepName(recruiter.getRecruiterRepName())
                .email(recruiter.getEmail())
                .phoneNumber(recruiter.getPhoneNumber())
                .recruiterType(recruiter.getRecruiterType())
                .companyName(recruiter.getCompanyName())
                .companyLogoUrl(recruiter.getCompanyLogoUrl())
                .companyDescription(recruiter.getCompanyDescription())
                .companyLocation(locationDTO)
                .companyWebsite(recruiter.getCompanyWebsite())
                .verificationStatus(recruiter.getVerificationStatus())
                .build();
    }

    // New method for public profile view - using LocationResponseDTO instead of LocationDTO
    public static PublicRecruiterDTO toPublicRecruiterDTO(Recruiters recruiter) {
        LocationResponseDTO locationResponseDTO = null;
        if (recruiter.getCompanyLocation() != null) {
            locationResponseDTO = LocationResponseDTO.builder()
                    .id(recruiter.getCompanyLocation().getId())
                    .name(recruiter.getCompanyLocation().getName())
                    .address(recruiter.getCompanyLocation().getAddress())
                    .city(recruiter.getCompanyLocation().getCity())
                    .state(recruiter.getCompanyLocation().getState())
                    .country(recruiter.getCompanyLocation().getCountry())
                    .postalCode(recruiter.getCompanyLocation().getPostalCode())
                    .latitude(recruiter.getCompanyLocation().getLatitude())
                    .longitude(recruiter.getCompanyLocation().getLongitude())
                    .placeId(recruiter.getCompanyLocation().getPlaceId())
                    .googleMapsUrl(recruiter.getCompanyLocation().getGoogleMapsUrl())
                    .build();
        }

        return PublicRecruiterDTO.builder()
                .id(recruiter.getId())
                .recruiterRepName(recruiter.getRecruiterRepName())
                .email(recruiter.getEmail())
                .phoneNumber(recruiter.getPhoneNumber())
                .recruiterType(recruiter.getRecruiterType())
                .companyName(recruiter.getCompanyName())
                .companyLogoUrl(recruiter.getCompanyLogoUrl())
                .companyDescription(recruiter.getCompanyDescription())
                .companyLocation(locationResponseDTO)
                .companyWebsite(recruiter.getCompanyWebsite())
                .verificationStatus(recruiter.getVerificationStatus())
                .build();
    }
}