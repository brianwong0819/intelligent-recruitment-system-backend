package com.event.recruitment.intelligent_recruitment_system.dto.response.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateExperience;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateExperienceDTO {
    private Long id;
    private JobType jobType;
    private String experienceText;

    // Constructor to convert entity to DTO
    public CandidateExperienceDTO(CandidateExperience experience) {
        this.id = experience.getId();
        this.jobType = experience.getJobType();
        this.experienceText = experience.getExperienceText();
    }
}