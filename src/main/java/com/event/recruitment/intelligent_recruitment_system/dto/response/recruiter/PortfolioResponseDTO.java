package com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponseDTO {
    private Integer id;
    private String eventName;
    private LocalDate eventStartDate;
    private LocalDate eventEndDate;
    private String eventDescription;
    private LocalDateTime uploadedAt;
    private List<EventMediaDTO> eventMedia = new ArrayList<>();
}