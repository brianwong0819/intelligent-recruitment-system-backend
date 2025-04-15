package com.event.recruitment.intelligent_recruitment_system.controller.candidate;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.candidate.JobApplicationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.candidate.WithdrawApplicationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.JobApplicationResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.service.job.JobApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidates/applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    @PostMapping
    public ResponseEntity<Response<JobApplicationResponseDTO>> applyForJob(
            @Valid @RequestBody JobApplicationRequest request) {
        Response<JobApplicationResponseDTO> response = jobApplicationService.applyForJob(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping
    public ResponseEntity<Response<List<JobApplicationResponseDTO>>> getCandidateApplications() {
        Response<List<JobApplicationResponseDTO>> response = jobApplicationService.getCandidateApplications();
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<Response<JobApplicationResponseDTO>> getApplicationDetails(
            @PathVariable Long applicationId) {
        Response<JobApplicationResponseDTO> response = jobApplicationService.getApplicationDetails(applicationId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Response<?>> withdrawApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody WithdrawApplicationRequest request) {
        Response<?> response = jobApplicationService.withdrawApplication(applicationId, request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<Response<JobApplicationResponseDTO>> getApplicationsByGroup(
            @PathVariable String groupId) {
        Response<JobApplicationResponseDTO> response = jobApplicationService.getApplicationsByGroup(groupId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @DeleteMapping("/group/{groupId}")
    public ResponseEntity<Response<?>> withdrawApplicationsByGroup(
            @PathVariable String groupId,
            @Valid @RequestBody WithdrawApplicationRequest request) {
        Response<?> response = jobApplicationService.withdrawApplicationsByGroup(groupId, request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}