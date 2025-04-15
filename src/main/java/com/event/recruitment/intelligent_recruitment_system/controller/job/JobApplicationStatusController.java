package com.event.recruitment.intelligent_recruitment_system.controller.job;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.job.UpdateApplicationStatusRequest;
import com.event.recruitment.intelligent_recruitment_system.service.job.JobApplicationStatusUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jobs/applications")
public class JobApplicationStatusController {

    private final JobApplicationStatusUpdateService jobApplicationStatusUpdateService;

    @PutMapping("/{applicationId}/status")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> updateApplicationStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateApplicationStatusRequest request) {
        try {
            Response<?> response = jobApplicationStatusUpdateService.updateApplicationStatus(
                    applicationId, request.getStatus());
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error updating application status: " + e.getMessage(), null));
        }
    }

    @PutMapping("/group/{groupId}/status")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> updateApplicationGroupStatus(
            @PathVariable String groupId,
            @Valid @RequestBody UpdateApplicationStatusRequest request) {
        try {
            Response<?> response = jobApplicationStatusUpdateService.updateApplicationGroupStatus(
                    groupId, request.getStatus());
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error updating application group status: " + e.getMessage(), null));
        }
    }
}