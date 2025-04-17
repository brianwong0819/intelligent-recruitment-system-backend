package com.event.recruitment.intelligent_recruitment_system.util;

import com.event.recruitment.intelligent_recruitment_system.dto.response.admin.RecruiterSummaryDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Recruiters;
import org.springframework.stereotype.Component;

@Component
public class AdminMapper {

    /**
     * Maps Recruiters entity to RecruiterSummaryDTO
     * @param recruiter The recruiter entity
     * @return RecruiterSummaryDTO
     */
    public RecruiterSummaryDTO mapToRecruiterSummaryDTO(Recruiters recruiter) {
        return RecruiterSummaryDTO.builder()
                .id(recruiter.getId())
                .username(recruiter.getUsername())
                .recruiterRepName(recruiter.getRecruiterRepName())
                .email(recruiter.getEmail())
                .phoneNumber(recruiter.getPhoneNumber())
                .recruiterType(recruiter.getRecruiterType())
                .companyName(recruiter.getCompanyName())
                .companyLogoUrl(recruiter.getCompanyLogoUrl())
                .verificationStatus(recruiter.getVerificationStatus())
                .createdAt(recruiter.getCreatedAt())
                .build();
    }
}