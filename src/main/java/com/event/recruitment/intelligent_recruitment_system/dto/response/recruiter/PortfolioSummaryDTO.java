package com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummaryDTO {
    private Integer id;
    private String eventName;
    private LocalDate eventStartDate;
    private LocalDate eventEndDate;
    private String eventDescription;
    private LocalDateTime uploadedAt;
    private String coverImageUrl; // First media URL as cover
    private Integer mediaCount;
}