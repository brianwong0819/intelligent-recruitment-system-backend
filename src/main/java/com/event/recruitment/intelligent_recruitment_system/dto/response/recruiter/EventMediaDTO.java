package com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMediaDTO {
    private Integer id;
    private String mediaUrl;
    private LocalDateTime uploadedAt;
}