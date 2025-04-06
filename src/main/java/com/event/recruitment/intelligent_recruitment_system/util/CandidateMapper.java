package com.event.recruitment.intelligent_recruitment_system.util;

import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;

public class CandidateMapper {

    public static CandidateResponseDTO toCandidateResponseDTO(Candidates candidate) {
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
                .preferredLocation(candidate.getPreferredLocation())
                .availability(candidate.getAvailability()!= null ? candidate.getAvailability().toString() : null)
                .bio(candidate.getBio())
                .resumeUrl(candidate.getResumeUrl())
                .build();
    }
}