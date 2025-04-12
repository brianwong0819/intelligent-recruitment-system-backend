// src/main/java/com/event/recruitment/intelligent_recruitment_system/controller/job/JobController.java

package com.event.recruitment.intelligent_recruitment_system.controller.job;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.job.ChangeJobStatusRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.job.CreateJobRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.job.UpdateJobRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.service.job.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECRUITER')")
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<Response<?>> createJob(@Valid @RequestBody CreateJobRequest request) {
        try {
            Response<JobResponseDTO> response = jobService.createJob(request);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error creating job: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<?>> getJobById(@PathVariable("id") Long jobId) {
        try {
            Response<JobResponseDTO> response = jobService.getJobById(jobId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error retrieving job: " + e.getMessage(), null));
        }
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<Response<?>> getJobsByProjectId(@PathVariable Long projectId) {
        try {
            Response<List<JobResponseDTO>> response = jobService.getJobsByProjectId(projectId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error retrieving jobs: " + e.getMessage(), null));
        }
    }

    @GetMapping
    public ResponseEntity<Response<?>> getAllJobs() {
        try {
            Response<List<JobResponseDTO>> response = jobService.getAllJobs();
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error retrieving jobs: " + e.getMessage(), null));
        }
    }

    /**
     * Update job details
     *
     * @param jobId   The ID of the job to update
     * @param request The updated job details
     * @return Response with updated job details
     */
    @PutMapping("/{jobId}")
    public ResponseEntity<Response<?>> updateJob(
            @PathVariable Long jobId,
            @Valid @RequestBody UpdateJobRequest request) {
        try {
            Response<JobResponseDTO> response = jobService.updateJob(jobId, request);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500,
                    "Error updating job: " + e.getMessage(), null));
        }
    }

    @PutMapping("/{jobId}/status")
    public ResponseEntity<Response<?>> changeJobStatus(
            @PathVariable Long jobId,
            @Valid @RequestBody ChangeJobStatusRequest request) {
        try {
            // Ensure the jobId in the path matches the jobId in the request body
            request.setJobId(jobId);

            Response<JobResponseDTO> response = jobService.changeJobStatus(request);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500,
                    "Error changing job status: " + e.getMessage(), null));
        }
    }
}