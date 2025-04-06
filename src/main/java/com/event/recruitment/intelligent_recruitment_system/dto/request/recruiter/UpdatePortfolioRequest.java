package com.event.recruitment.intelligent_recruitment_system.dto.request.recruiter;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePortfolioRequest {

    @NotNull(message = "Portfolio ID is required")
    private Integer id;

    @Size(max = 255, message = "Event name cannot exceed 255 characters")
    private String eventName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate eventStartDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate eventEndDate;

    @Size(max = 1000, message = "Event description cannot exceed 1000 characters")
    private String eventDescription;
}