// src/main/java/com/event/recruitment/intelligent_recruitment_system/service/job/JobScheduleService.java

package com.event.recruitment.intelligent_recruitment_system.service.job;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.job.CreateJobScheduleRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobScheduleResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobLocation;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobSchedule;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobScheduleDate;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.Jobs;
import com.event.recruitment.intelligent_recruitment_system.model.entity.location.Location;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobLocationStatus;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobLocationRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobScheduleDateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobScheduleRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.location.LocationRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import com.event.recruitment.intelligent_recruitment_system.util.JobScheduleMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobScheduleService {
    private final JobScheduleRepository jobScheduleRepository;
    private final JobScheduleDateRepository jobScheduleDateRepository;
    private final JobLocationRepository jobLocationRepository;
    private final JobRepository jobsRepository;
    private final LocationRepository locationsRepository;
    private final SecurityUtil securityUtil;
    private final JobScheduleMapper jobScheduleMapper;

    /**
     * Create a new job schedule
     * @param request Job schedule creation request
     * @return Response with created job schedule
     */
    @Transactional
    public Response<JobScheduleResponseDTO> createJobSchedule(CreateJobScheduleRequest request) {
        // Validate job exists and belongs to current recruiter
        Jobs job = jobsRepository.findById(request.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));

        // Validate date range
        validateDateRange(request);

        // Create JobSchedule entity
        JobSchedule jobSchedule = JobSchedule.builder()
                .job(job)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .hoursOfRestTime(request.getHoursOfRestTime())
                .numPositions(request.getNumPositions())
                .build();

        // Save job schedule
        jobSchedule = jobScheduleRepository.save(jobSchedule);

        // Create and save schedule dates
        if (request.getScheduleDates() != null) {
            final JobSchedule currentJobSchedule = jobSchedule;
            List<JobScheduleDate> scheduleDates = request.getScheduleDates().stream()
                    .map(dateRequest -> {
                        JobScheduleDate scheduleDate = JobScheduleDate.builder()
                                .jobSchedule(currentJobSchedule)
                                .workDate(dateRequest.getWorkDate())
                                .build();

                        // Create and save job locations for this schedule date
                        if (dateRequest.getJobLocations() != null) {
                            List<JobLocation> jobLocations = dateRequest.getJobLocations().stream()
                                    .map(locationRequest -> {
                                        // Validate location exists
                                        Location location = locationsRepository.findById(locationRequest.getLocationId())
                                                .orElseThrow(() -> new RuntimeException("Location not found"));

                                        // Determine positions needed
                                        Integer positionsNeeded = locationRequest.getPositionsNeeded() != null
                                                ? locationRequest.getPositionsNeeded()
                                                : currentJobSchedule.getNumPositions();

                                        return JobLocation.builder()
                                                .job(job)
                                                .location(location)
                                                .jobScheduleDate(scheduleDate)
                                                .positionsNeeded(positionsNeeded)
                                                .positionsFilled(0)
                                                .status(JobLocationStatus.OPEN)
                                                .notes(locationRequest.getNotes())
                                                .build();
                                    })
                                    .collect(Collectors.toList());

                            scheduleDate.setJobLocations(jobLocations);
                        }

                        return scheduleDate;
                    })
                    .collect(Collectors.toList());

            jobSchedule.setScheduleDates(jobScheduleDateRepository.saveAll(scheduleDates));
        }

        // Convert and return response
        JobScheduleResponseDTO responseDTO = jobScheduleMapper.toResponseDTO(jobSchedule);
        return new Response<>(HttpStatus.CREATED.value(), "Job schedule created successfully", responseDTO);
    }

    /**
     * Retrieve job schedules for a specific job
     * @param jobId Job ID
     * @return Response with list of job schedules
     */
    public Response<List<JobScheduleResponseDTO>> getJobSchedulesByJob(Long jobId) {
        // Validate job exists
        Jobs job = jobsRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        List<JobSchedule> schedules = jobScheduleRepository.findByJobId(jobId);
        List<JobScheduleResponseDTO> responseDTOs = schedules.stream()
                .map(jobScheduleMapper::toResponseDTO)
                .collect(Collectors.toList());

        return new Response<>(HttpStatus.OK.value(), "Job schedules retrieved successfully", responseDTOs);
    }

    /**
     * Retrieve a specific job schedule by ID
     * @param scheduleId Schedule ID
     * @return Response with job schedule details
     */
    public Response<JobScheduleResponseDTO> getJobScheduleById(Long scheduleId) {
        JobSchedule schedule = jobScheduleRepository.findByIdWithDetails(scheduleId)
                .orElseThrow(() -> new RuntimeException("Job schedule not found"));

        JobScheduleResponseDTO responseDTO = jobScheduleMapper.toResponseDTO(schedule);
        return new Response<>(HttpStatus.OK.value(), "Job schedule retrieved successfully", responseDTO);
    }

    /**
     * Validate date range for job schedule
     * @param request Job schedule creation request
     */
    private void validateDateRange(CreateJobScheduleRequest request) {
        // Validate start date is today or in the future
        LocalDate today = LocalDate.now();
        if (request.getStartDate().isBefore(today)) {
            throw new IllegalArgumentException("Start date must be today or in the future");
        }

        // If end date is provided, it must be after or equal to start date
        if (request.getEndDate() != null) {
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("End date must be after or equal to start date");
            }
        }

        // Validate schedule dates (if provided) are within the job schedule date range
        validateScheduleDates(request);
    }

    /**
     * Validate schedule dates are within the job schedule date range
     * @param request Job schedule creation request
     */
    private void validateScheduleDates(CreateJobScheduleRequest request) {
        if (request.getScheduleDates() != null) {
            for (CreateJobScheduleRequest.CreateJobScheduleDateRequest dateRequest : request.getScheduleDates()) {
                if (dateRequest.getWorkDate().isBefore(request.getStartDate()) ||
                        (request.getEndDate() != null && dateRequest.getWorkDate().isAfter(request.getEndDate()))) {
                    throw new IllegalArgumentException("Schedule dates must be within the job schedule date range");
                }
            }
        }
    }

    /**
     * Update an existing job schedule
     * @param scheduleId Schedule ID
     * @param request Job schedule update request
     * @return Response with updated job schedule
     */
    @Transactional
    public Response<JobScheduleResponseDTO> updateJobSchedule(Long scheduleId, CreateJobScheduleRequest request) {
        // Find existing job schedule
        JobSchedule existingSchedule = jobScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Job schedule not found"));

        // Validate job exists and belongs to current recruiter
        Jobs job = jobsRepository.findById(request.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));

        // Validate date range
        validateDateRange(request);

        // Update basic schedule details
        existingSchedule.setStartDate(request.getStartDate());
        existingSchedule.setEndDate(request.getEndDate());
        existingSchedule.setStartTime(request.getStartTime());
        existingSchedule.setEndTime(request.getEndTime());
        existingSchedule.setHoursOfRestTime(request.getHoursOfRestTime());
        existingSchedule.setNumPositions(request.getNumPositions());

        // Clear existing schedule dates
        existingSchedule.getScheduleDates().clear();

        // Create and save new schedule dates
        if (request.getScheduleDates() != null) {
            JobSchedule finalExistingSchedule = existingSchedule;
            request.getScheduleDates().forEach(dateRequest -> {
                // Create schedule date
                JobScheduleDate scheduleDate = JobScheduleDate.builder()
                        .jobSchedule(finalExistingSchedule)
                        .workDate(dateRequest.getWorkDate())
                        .build();

                // Create job locations for this schedule date
                if (dateRequest.getJobLocations() != null) {
                    List<JobLocation> jobLocations = dateRequest.getJobLocations().stream()
                            .map(locationRequest -> {
                                // Validate location exists
                                Location location = locationsRepository.findById(locationRequest.getLocationId())
                                        .orElseThrow(() -> new RuntimeException("Location not found"));

                                // Determine positions needed
                                Integer positionsNeeded = locationRequest.getPositionsNeeded() != null
                                        ? locationRequest.getPositionsNeeded()
                                        : finalExistingSchedule.getNumPositions();

                                // Create job location
                                JobLocation jobLocation = JobLocation.builder()
                                        .job(job)
                                        .location(location)
                                        .jobScheduleDate(scheduleDate)
                                        .positionsNeeded(positionsNeeded)
                                        .positionsFilled(0)
                                        .status(JobLocationStatus.OPEN)
                                        .notes(locationRequest.getNotes())
                                        .build();

                                return jobLocation;
                            })
                            .collect(Collectors.toList());

                    // Set job locations for the schedule date
                    scheduleDate.setJobLocations(jobLocations);
                }

                // Add schedule date to existing schedule
                finalExistingSchedule.getScheduleDates().add(scheduleDate);
            });
        }

        // Save updated schedule
        existingSchedule = jobScheduleRepository.save(existingSchedule);

        // Convert and return response
        JobScheduleResponseDTO responseDTO = jobScheduleMapper.toResponseDTO(existingSchedule);
        return new Response<>(HttpStatus.OK.value(), "Job schedule updated successfully", responseDTO);
    }

    /**
     * Delete a job schedule
     * @param scheduleId Schedule ID to delete
     * @return Response indicating successful deletion
     */
    @Transactional
    public Response<?> deleteJobSchedule(Long scheduleId) {
        // Find existing job schedule
        JobSchedule existingSchedule = jobScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Job schedule not found"));

        // Delete associated schedule dates and job locations
        jobScheduleDateRepository.deleteAll(existingSchedule.getScheduleDates());

        // Delete the job schedule
        jobScheduleRepository.delete(existingSchedule);

        return new Response<>(HttpStatus.OK.value(), "Job schedule deleted successfully", null);
    }

    /**
     * Find job schedules within a specific date range
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Response with list of job schedules
     */
    public Response<List<JobScheduleResponseDTO>> findSchedulesInDateRange(LocalDate startDate, LocalDate endDate) {
        List<JobSchedule> schedules = jobScheduleRepository.findSchedulesInDateRange(startDate, endDate);

        List<JobScheduleResponseDTO> responseDTOs = schedules.stream()
                .map(jobScheduleMapper::toResponseDTO)
                .collect(Collectors.toList());

        return new Response<>(HttpStatus.OK.value(), "Job schedules retrieved successfully", responseDTOs);
    }

    /**
     * Get available positions for a specific job schedule
     * @param scheduleId Schedule ID
     * @return Response with number of available positions
     */
    public Response<Integer> getAvailablePositions(Long scheduleId) {
        JobSchedule schedule = jobScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Job schedule not found"));

        int availablePositions = schedule.getScheduleDates().stream()
                .flatMap(date -> date.getJobLocations().stream())
                .mapToInt(location -> location.getPositionsNeeded() - location.getPositionsFilled())
                .sum();

        return new Response<>(HttpStatus.OK.value(), "Available positions retrieved successfully", availablePositions);
    }
}