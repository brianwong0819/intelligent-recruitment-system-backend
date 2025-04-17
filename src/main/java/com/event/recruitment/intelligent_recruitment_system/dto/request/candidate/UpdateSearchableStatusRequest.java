package com.event.recruitment.intelligent_recruitment_system.dto.request.candidate;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSearchableStatusRequest {
    @NotNull(message = "Searchable status cannot be null")
    private Boolean isSearchable;
}