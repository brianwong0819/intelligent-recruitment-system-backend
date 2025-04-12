// src/main/java/com/event/recruitment/intelligent_recruitment_system/dto/request/job/CreateJobScheduleRequest.java

package com.event.recruitment.intelligent_recruitment_system.dto.request.job;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
public class CreateJobScheduleRequest {
    @NotNull(message = "Job ID is required")
    private Long jobId;

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @DecimalMin(value = "0.0", inclusive = true, message = "Hours of rest time must be positive or zero")
    private BigDecimal hoursOfRestTime;

    @Positive(message = "Number of positions must be positive")
    @NotNull(message = "Number of positions is required")
    private Integer numPositions;

    @Valid
    private List<CreateJobScheduleDateRequest> scheduleDates;

    // Inner classes remain the same
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateJobScheduleDateRequest {
        @NotNull(message = "Work date is required")
        @FutureOrPresent(message = "Start date must be today or in the future")
        private LocalDate workDate;

        @Valid
        private List<CreateJobLocationRequest> jobLocations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateJobLocationRequest {
        @NotNull(message = "Location ID is required")
        private Long locationId;

        @Positive(message = "Positions needed must be positive")
        private Integer positionsNeeded;

        private String notes;
    }
}