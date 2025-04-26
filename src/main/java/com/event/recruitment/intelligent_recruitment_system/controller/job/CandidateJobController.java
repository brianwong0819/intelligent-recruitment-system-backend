package com.event.recruitment.intelligent_recruitment_system.controller.job;

import com.event.recruitment.intelligent_recruitment_system.dto.common.PagedResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobSummaryResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.service.job.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for job-related endpoints specific to candidates
 */
@RestController
@RequestMapping("/api/jobs/candidate")
@RequiredArgsConstructor
public class CandidateJobController {

    private final JobService jobService;

    /**
     * Get jobs near the candidate's preferred location
     * This endpoint is protected and only accessible to authenticated candidates
     *
     * @param distance Distance in kilometers to search around the candidate's preferred location
     * @param page Page number for pagination
     * @param size Page size for pagination
     * @param sortBy Field to sort by
     * @param sortDirection Direction to sort (asc/desc)
     * @return Paged response of jobs near the candidate's preferred location
     */
    @GetMapping("/near-me")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Response<PagedResponseDTO<JobSummaryResponseDTO>>> getJobsNearMe(
            @RequestParam(defaultValue = "10.0") Double distance,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        try {
            Response<PagedResponseDTO<JobSummaryResponseDTO>> response =
                    jobService.getJobsNearCandidateLocation(distance, page, size, sortBy, sortDirection);

            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving nearby jobs: " + e.getMessage(), null));
        }
    }
}