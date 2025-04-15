package com.event.recruitment.intelligent_recruitment_system.dto.request.job;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApplicationStatusRequest {

    @NotBlank(message = "Status cannot be empty")
    private String status;
}