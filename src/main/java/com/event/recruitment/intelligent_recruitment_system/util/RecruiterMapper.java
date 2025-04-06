package com.event.recruitment.intelligent_recruitment_system.util;

import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.RecruiterResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Recruiters;

public class RecruiterMapper {

    public static RecruiterResponseDTO toRecruiterResponseDTO(Recruiters recruiter) {
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
                .companyLocation(recruiter.getCompanyLocation())
                .companyWebsite(recruiter.getCompanyWebsite())
                .verificationStatus(recruiter.getVerificationStatus())
                .build();
    }
}