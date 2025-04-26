package com.event.recruitment.intelligent_recruitment_system.util;

import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobScheduleResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobLocation;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobSchedule;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobScheduleDate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JobScheduleMapper {

    /**
     * Convert JobSchedule entity to JobScheduleResponseDTO
     * @param jobSchedule Job schedule entity
     * @return JobScheduleResponseDTO
     */
    public JobScheduleResponseDTO toResponseDTO(JobSchedule jobSchedule) {
        if (jobSchedule == null) {
            return null;
        }

        return JobScheduleResponseDTO.builder()
                .id(jobSchedule.getId())
                .jobId(jobSchedule.getJob().getId())
                .startDate(jobSchedule.getStartDate())
                .endDate(jobSchedule.getEndDate())
                .startTime(jobSchedule.getStartTime())
                .endTime(jobSchedule.getEndTime())
                .hoursOfRestTime(jobSchedule.getHoursOfRestTime())
                .numPositions(jobSchedule.getNumPositions())
                .scheduleDates(mapScheduleDates(jobSchedule.getScheduleDates()))
                .build();
    }

    /**
     * Map list of JobScheduleDate entities to DTOs
     * @param scheduleDates List of job schedule date entities
     * @return List of JobScheduleDateResponseDTO
     */
    private List<JobScheduleResponseDTO.JobScheduleDateResponseDTO> mapScheduleDates(List<JobScheduleDate> scheduleDates) {
        if (scheduleDates == null) {
            return null;
        }

        return scheduleDates.stream()
                .map(this::mapScheduleDate)
                .collect(Collectors.toList());
    }

    /**
     * Map JobScheduleDate entity to DTO
     * @param scheduleDate Job schedule date entity
     * @return JobScheduleDateResponseDTO
     */
    private JobScheduleResponseDTO.JobScheduleDateResponseDTO mapScheduleDate(JobScheduleDate scheduleDate) {
        return JobScheduleResponseDTO.JobScheduleDateResponseDTO.builder()
                .id(scheduleDate.getId())
                .workDate(scheduleDate.getWorkDate())
                .jobLocations(mapJobLocations(scheduleDate.getJobLocations()))
                .build();
    }

    /**
     * Map list of JobLocation entities to DTOs
     * @param jobLocations List of job location entities
     * @return List of JobLocationResponseDTO
     */
    private List<JobScheduleResponseDTO.JobLocationResponseDTO> mapJobLocations(List<JobLocation> jobLocations) {
        if (jobLocations == null) {
            return null;
        }

        return jobLocations.stream()
                .map(this::mapJobLocation)
                .collect(Collectors.toList());
    }

    /**
     * Map JobLocation entity to DTO
     * @param jobLocation Job location entity
     * @return JobLocationResponseDTO
     */
    private JobScheduleResponseDTO.JobLocationResponseDTO mapJobLocation(JobLocation jobLocation) {
        return JobScheduleResponseDTO.JobLocationResponseDTO.builder()
                .id(jobLocation.getId())
                .locationId(jobLocation.getLocation().getId())
                .locationName(jobLocation.getLocation().getName())
                .positionsNeeded(jobLocation.getPositionsNeeded())
                .positionsFilled(jobLocation.getPositionsFilled())
                .status(jobLocation.getStatus().toString())
                .notes(jobLocation.getNotes())
                .build();
    }
}