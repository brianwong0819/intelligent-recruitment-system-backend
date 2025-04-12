package com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for returning project statistics
 */
@Data
@Builder
public class ProjectStatsDTO {
    private Long projectId;
    private String projectName;

    // Job statistics
    private Integer totalJobs;

    // Location statistics
    private Integer totalLocations;
    private Integer totalUniqueLocations;

    // Manpower statistics
    private Integer totalPositionsNeeded;
    private Integer totalPositionsFilled;

    // Schedule statistics
    private Integer totalWorkingDays;

    // Status statistics
    private Integer openJobs;
    private Integer filledJobs;
    private Integer partiallyFilledJobs;
    private Integer cancelledJobs;
}