package com.event.recruitment.intelligent_recruitment_system.controller.job;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.job.ApplicationGroupStatusUpdateRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.job.ApplicationStatusUpdateRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.ApplicantSummaryDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.ApplicantsResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.service.job.JobApplicationManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/recruiters/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECRUITER')")
public class JobApplicationManagementController {

    private final JobApplicationManagementService jobApplicationManagementService;

    /**
     * Get all applicants for a job
     * @param jobId The job ID
     * @param status Optional status filter
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Field to sort by
     * @param sortDir Sort direction (asc/desc)
     * @return List of applicants with pagination info
     */
    @GetMapping("/{jobId}/applicants")
    public ResponseEntity<Response<ApplicantsResponseDTO>> getJobApplicants(
            @PathVariable Long jobId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        try {
            Response<ApplicantsResponseDTO> response;

            if (status != null && !status.isEmpty()) {
                // Use filtered method if status is provided
                response = jobApplicationManagementService.getJobApplicantsByStatus(
                        jobId, status, page, size, sortBy, sortDir);
            } else {
                // Use original method if no status filter
                response = jobApplicationManagementService.getJobApplicants(
                        jobId, page, size, sortBy, sortDir);
            }

            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving job applicants: " + e.getMessage(), null));
        }
    }

    /**
     * Get the total number of applicants for a job
     * @param jobId The job ID
     * @return The total count
     */
    @GetMapping("/{jobId}/applicants/count")
    public ResponseEntity<Response<Long>> getJobApplicantsCount(@PathVariable Long jobId) {
        try {
            Response<Long> response = jobApplicationManagementService.getJobApplicantsCount(jobId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving applicant count: " + e.getMessage(), null));
        }
    }

    /**
     * Get applicant statistics for a job
     * @param jobId The job ID
     * @return Statistics for different application statuses
     */
    @GetMapping("/{jobId}/applicants/stats")
    public ResponseEntity<Response<Map<String, Long>>> getJobApplicantsStats(@PathVariable Long jobId) {
        try {
            Response<Map<String, Long>> response = jobApplicationManagementService.getJobApplicantsStats(jobId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving applicant statistics: " + e.getMessage(), null));
        }
    }

    /**
     * Get detailed information about an applicant
     * @param applicationId The application ID
     * @return Detailed applicant information
     */
    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<Response<ApplicantSummaryDTO>> getApplicantDetails(
            @PathVariable Long applicationId) {

        try {
            Response<ApplicantSummaryDTO> response =
                    jobApplicationManagementService.getApplicantDetails(applicationId);

            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving applicant details: " + e.getMessage(), null));
        }
    }

    /**
     * Update the status of a job application
     * @param applicationId The application ID
     * @param request The status update request
     * @return Success or failure response
     */
    @PutMapping("/applications/{applicationId}/status")
    public ResponseEntity<Response<?>> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestBody @Valid ApplicationStatusUpdateRequest request) {

        try {
            Response<?> response =
                    jobApplicationManagementService.updateApplicationStatus(applicationId, request.getStatus());

            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error updating application status: " + e.getMessage(), null));
        }
    }

    /**
     * Update the status of all applications in a group
     * @param request The group status update request
     * @return Success or failure response
     */
    @PutMapping("/applications/group/status")
    public ResponseEntity<Response<?>> updateApplicationGroupStatus(
            @RequestBody @Valid ApplicationGroupStatusUpdateRequest request) {

        try {
            Response<?> response =
                    jobApplicationManagementService.updateApplicationGroupStatus(
                            request.getGroupId(), request.getStatus());

            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error updating application group status: " + e.getMessage(), null));
        }
    }
}