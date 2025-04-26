package com.event.recruitment.intelligent_recruitment_system.controller.job;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.service.job.JobInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jobs")
public class JobInteractionController {

    private final JobInteractionService jobInteractionService;

    /**
     * Save a job for the current candidate
     * @param jobId The job ID to save
     * @return Response with success or failure message
     */
    @PostMapping("/{jobId}/save")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Response<?>> saveJob(@PathVariable Long jobId) {
        Response<?> response = jobInteractionService.saveJob(jobId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Remove a job from the current candidate's saved list
     * @param jobId The job ID to unsave
     * @return Response with success or failure message
     */
    @DeleteMapping("/{jobId}/save")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Response<?>> unsaveJob(@PathVariable Long jobId) {
        Response<?> response = jobInteractionService.unsaveJob(jobId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Record a job view for the current candidate
     * @param jobId The job ID that was viewed
     * @return Response with success or failure message
     */
    @PostMapping("/{jobId}/view")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Response<?>> viewJob(@PathVariable Long jobId) {
        Response<?> response = jobInteractionService.viewJob(jobId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Get the current candidate's interaction status for a specific job
     * @param jobId The job ID to check
     * @return Response with saved and viewed status
     */
    @GetMapping("/{jobId}/interaction-status")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Response<?>> getInteractionStatus(@PathVariable Long jobId) {
        Response<?> response = jobInteractionService.getInteractionStatus(jobId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Get all jobs saved by the current candidate
     * @return Response with list of saved jobs
     */
    @GetMapping("/saved")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Response<?>> getSavedJobs() {
        Response<?> response = jobInteractionService.getSavedJobs();
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Get all jobs viewed by the current candidate
     * @return Response with list of viewed jobs
     */
    @GetMapping("/viewed")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Response<?>> getViewedJobs() {
        Response<?> response = jobInteractionService.getViewedJobs();
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Get view statistics for a specific job (recruiter only)
     * @param jobId The job ID to get statistics for
     * @return Response with view statistics
     */
    @GetMapping("/{jobId}/view-statistics")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> getJobViewStatistics(@PathVariable Long jobId) {
        Response<?> response = jobInteractionService.getJobViewStatistics(jobId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}