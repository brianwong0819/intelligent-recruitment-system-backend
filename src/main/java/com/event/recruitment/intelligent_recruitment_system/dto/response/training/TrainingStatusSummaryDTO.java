package com.event.recruitment.intelligent_recruitment_system.dto.response.training;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingStatusSummaryDTO {
    private Long jobId;
    private String jobTitle;
    private Integer totalCandidates;
    private Integer viewedCount;
    private Integer completedCount;
    private Integer notStartedCount;
    private Double completionPercentage;
}