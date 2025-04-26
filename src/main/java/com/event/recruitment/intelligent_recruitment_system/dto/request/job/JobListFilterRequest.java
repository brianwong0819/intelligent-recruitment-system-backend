package com.event.recruitment.intelligent_recruitment_system.dto.request.job;

import com.event.recruitment.intelligent_recruitment_system.model.enums.JobTitleType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.ListingTimeFilter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobListFilterRequest {
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;

    private JobTitleType jobTitleType;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private ListingTimeFilter listingTime;

    // Geographic filtering (mutually exclusive)
    private Double latitude;
    private Double longitude;
    private Double distance; // in kilometers
    private String location; // Single field for searching city or state

    private Double radius; // in kilometers, to search around the candidate's preferred location
    private Boolean usePreferredLocation; // flag to indicate if candidate's preferred location should be used

    // Date filtering
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}