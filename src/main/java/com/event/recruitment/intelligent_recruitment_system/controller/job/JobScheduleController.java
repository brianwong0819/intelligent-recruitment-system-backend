package com.event.recruitment.intelligent_recruitment_system.controller.job;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.job.CreateJobScheduleRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobScheduleResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.service.job.JobScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/job-schedules")
@RequiredArgsConstructor
public class JobScheduleController {

    private final JobScheduleService jobScheduleService;

    /**
     * Create a new job schedule
     * @param request Job schedule creation request
     * @return ResponseEntity with created job schedule
     */
    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<JobScheduleResponseDTO>> createJobSchedule(
            @Valid @RequestBody CreateJobScheduleRequest request) {
        Response<JobScheduleResponseDTO> response = jobScheduleService.createJobSchedule(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Get job schedules for a specific job
     * @param jobId Job ID
     * @return ResponseEntity with list of job schedules
     */
    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<List<JobScheduleResponseDTO>>> getJobSchedulesByJob(
            @PathVariable Long jobId) {
        Response<List<JobScheduleResponseDTO>> response = jobScheduleService.getJobSchedulesByJob(jobId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Get a specific job schedule by ID
     * @param scheduleId Schedule ID
     * @return ResponseEntity with job schedule details
     */
    @GetMapping("/{scheduleId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<JobScheduleResponseDTO>> getJobScheduleById(
            @PathVariable Long scheduleId) {
        Response<JobScheduleResponseDTO> response = jobScheduleService.getJobScheduleById(scheduleId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Update an existing job schedule
     * @param scheduleId Schedule ID to update
     * @param request Job schedule update request
     * @return ResponseEntity with updated job schedule
     */
    @PutMapping("/{scheduleId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<JobScheduleResponseDTO>> updateJobSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody CreateJobScheduleRequest request) {
        Response<JobScheduleResponseDTO> response = jobScheduleService.updateJobSchedule(scheduleId, request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Delete a job schedule
     * @param scheduleId Schedule ID to delete
     * @return ResponseEntity indicating successful deletion
     */
    @DeleteMapping("/{scheduleId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> deleteJobSchedule(
            @PathVariable Long scheduleId) {
        Response<?> response = jobScheduleService.deleteJobSchedule(scheduleId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Find job schedules within a specific date range
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return ResponseEntity with list of job schedules
     */
    @GetMapping("/date-range")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<List<JobScheduleResponseDTO>>> findSchedulesInDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Response<List<JobScheduleResponseDTO>> response = jobScheduleService.findSchedulesInDateRange(startDate, endDate);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Get available positions for a specific job schedule
     * @param scheduleId Schedule ID
     * @return ResponseEntity with number of available positions
     */
    @GetMapping("/{scheduleId}/available-positions")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<Integer>> getAvailablePositions(
            @PathVariable Long scheduleId) {
        Response<Integer> response = jobScheduleService.getAvailablePositions(scheduleId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}