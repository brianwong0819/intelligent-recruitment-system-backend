package com.event.recruitment.intelligent_recruitment_system.controller.job;

import com.event.recruitment.intelligent_recruitment_system.dto.common.PagedResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.job.JobListFilterRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobSummaryResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.service.job.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs/public")
@RequiredArgsConstructor
public class PublicJobController {

    private final JobService jobService;

    /**
     * Get filtered list of jobs with pagination
     * This endpoint is public and accessible without authentication
     */
    @GetMapping
    public ResponseEntity<Response<PagedResponseDTO<JobSummaryResponseDTO>>> getJobsWithFilters(
            JobListFilterRequest filterRequest) {
        try {
            Response<PagedResponseDTO<JobSummaryResponseDTO>> response = jobService.getJobsWithFilters(filterRequest);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving jobs: " + e.getMessage(), null));
        }
    }

    /**
     * Get detailed job information by ID
     * This endpoint is public and accessible without authentication
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<Response<JobResponseDTO>> getPublicJobDetails(@PathVariable Long jobId) {
        try {
            Response<JobResponseDTO> response = jobService.getPublicJobDetails(jobId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving job details: " + e.getMessage(), null));
        }
    }

    /**
     * Get jobs within a specific distance from provided coordinates
     */
    @GetMapping("/nearby")
    public ResponseEntity<Response<PagedResponseDTO<JobSummaryResponseDTO>>> getJobsNearby(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double distance,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        try {
            // Create filter request with location parameters
            JobListFilterRequest filterRequest = JobListFilterRequest.builder()
                    .page(page)
                    .size(size)
                    .sortBy(sortBy)
                    .sortDirection(sortDirection)
                    .latitude(latitude)
                    .longitude(longitude)
                    .distance(distance)
                    .build();

            Response<PagedResponseDTO<JobSummaryResponseDTO>> response =
                    jobService.getJobsWithFilters(filterRequest);

            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving nearby jobs: " + e.getMessage(), null));
        }
    }
}