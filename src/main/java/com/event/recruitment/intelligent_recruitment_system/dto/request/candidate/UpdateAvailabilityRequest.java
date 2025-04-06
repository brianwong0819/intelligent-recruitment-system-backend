package com.event.recruitment.intelligent_recruitment_system.dto.request.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.enums.Availability;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateAvailabilityRequest {
    @NotNull
    private Availability availabilityType;

    private List<LocalDate> customDates;
}