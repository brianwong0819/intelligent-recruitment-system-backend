package com.event.recruitment.intelligent_recruitment_system.dto.response.candidate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for returning statistics about a candidate's profile
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfileStatsDTO {

    private Long candidateId;
    private long experienceCount;
    private boolean hasResume;
    private long workingPhotoCount;
    private long comcardCount;
    private String preferredLocation;
}