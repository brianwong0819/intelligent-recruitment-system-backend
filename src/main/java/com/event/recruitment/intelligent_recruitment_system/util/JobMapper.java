// src/main/java/com/event/recruitment/intelligent_recruitment_system/util/JobMapper.java

package com.event.recruitment.intelligent_recruitment_system.util;

import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobSummaryResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobLocation;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobSchedule;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobScheduleDate;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.Jobs;
import com.event.recruitment.intelligent_recruitment_system.model.enums.RecruiterType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JobMapper {

    public JobResponseDTO toResponseDTO(Jobs job) {
        return JobResponseDTO.builder()
                .id(job.getId())
                .projectId(job.getProject().getId())
                .projectName(job.getProject().getName())
                .title(job.getTitle())
                .jobTitleType(job.getJobTitleType())
                .jobScope(job.getJobScope())
                .requirements(job.getRequirements())
                .salary(job.getSalary())
                .paymentTerms(job.getPaymentTerms())
                .salaryType(job.getSalaryType())
                .benefits(job.getBenefits())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .build();
    }

    public JobSummaryResponseDTO toSummaryDTO(Jobs job) {
        // Get all job schedules
        List<JobSchedule> schedules = job.getJobSchedules();

        // Calculate earliest start date and latest end date
        LocalDate earliestStartDate = null;
        LocalDate latestEndDate = null;
        LocalTime startTime = null;
        LocalTime endTime = null;

        if (schedules != null && !schedules.isEmpty()) {
            // Find earliest start time and latest end time across all schedules
            startTime = schedules.stream()
                    .map(JobSchedule::getStartTime)
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            endTime = schedules.stream()
                    .map(JobSchedule::getEndTime)
                    .max(Comparator.naturalOrder())
                    .orElse(null);

            // Find earliest start date across all schedules
            earliestStartDate = schedules.stream()
                    .flatMap(schedule -> schedule.getScheduleDates().stream())
                    .map(JobScheduleDate::getWorkDate)
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            // Find latest end date across all schedules
            latestEndDate = schedules.stream()
                    .flatMap(schedule -> schedule.getScheduleDates().stream())
                    .map(JobScheduleDate::getWorkDate)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
        }

        // Get unique location names
        List<String> locations = new ArrayList<>();
        if (schedules != null) {
            locations = schedules.stream()
                    .flatMap(schedule -> schedule.getScheduleDates().stream())
                    .flatMap(scheduleDate -> scheduleDate.getJobLocations().stream())
                    .map(jobLocation -> jobLocation.getLocation().getName())
                    .distinct()
                    .collect(Collectors.toList());
        }

        // Calculate total and available positions
        int totalPositions = 0;
        int availablePositions = 0;

        if (schedules != null) {
            for (JobSchedule schedule : schedules) {
                for (JobScheduleDate scheduleDate : schedule.getScheduleDates()) {
                    for (JobLocation jobLocation : scheduleDate.getJobLocations()) {
                        totalPositions += jobLocation.getPositionsNeeded() != null ? jobLocation.getPositionsNeeded() : 0;
                        availablePositions += (jobLocation.getPositionsNeeded() != null && jobLocation.getPositionsFilled() != null)
                                ? jobLocation.getPositionsNeeded() - jobLocation.getPositionsFilled() : 0;
                    }
                }
            }
        }

        // Get company information
        String companyName;
        String companyLogoUrl = job.getProject().getRecruiter().getCompanyLogoUrl();
        RecruiterType recruiterType = job.getProject().getRecruiter().getRecruiterType();

        // FIXED LOGIC: If company name is null/empty OR recruiter type is individual, use recruiter representative name
        String originalCompanyName = job.getProject().getRecruiter().getCompanyName();
        if ((originalCompanyName == null || originalCompanyName.trim().isEmpty()) ||
                RecruiterType.INDIVIDUAL.equals(recruiterType)) {
            companyName = job.getProject().getRecruiter().getRecruiterRepName();
        } else {
            companyName = originalCompanyName;
        }

        return JobSummaryResponseDTO.builder()
                .id(job.getId())
                .title(job.getTitle())
                .jobTitleType(job.getJobTitleType())
                .companyName(companyName)
                .companyLogoUrl(companyLogoUrl)
                .recruiterType(recruiterType)
                .locations(locations)
                .salary(job.getSalary())
                .salaryType(job.getSalaryType())
                .paymentTerms(job.getPaymentTerms())
                .createdAt(job.getCreatedAt())
                .earliestStartDate(earliestStartDate)
                .latestEndDate(latestEndDate)
                .startTime(startTime)
                .endTime(endTime)
                .totalPositions(totalPositions)
                .availablePositions(availablePositions)
                .build();
    }
}