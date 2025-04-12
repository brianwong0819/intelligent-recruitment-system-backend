// src/main/java/com/event/recruitment/intelligent_recruitment_system/dto/response/job/JobScheduleResponseDTO.java

package com.event.recruitment.intelligent_recruitment_system.dto.response.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobScheduleResponseDTO {
    private Long id;
    private Long jobId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal hoursOfRestTime;
    private Integer numPositions;
    private List<JobScheduleDateResponseDTO> scheduleDates;

    // Inner classes remain the same
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobScheduleDateResponseDTO {
        private Long id;
        private LocalDate workDate;
        private List<JobLocationResponseDTO> jobLocations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobLocationResponseDTO {
        private Long id;
        private Long locationId;
        private String locationName;
        private Integer positionsNeeded;
        private Integer positionsFilled;
        private String status;
        private String notes;
    }
}