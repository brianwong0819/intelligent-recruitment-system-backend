package com.event.recruitment.intelligent_recruitment_system.dto.response.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobInteractionStatusResponse {
    private boolean saved;
    private boolean viewed;
}