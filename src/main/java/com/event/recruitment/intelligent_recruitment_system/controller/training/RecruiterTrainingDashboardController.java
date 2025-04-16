// Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/controller/training/RecruiterTrainingDashboardController.java

package com.event.recruitment.intelligent_recruitment_system.controller.training;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.service.training.CandidateTrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recruiters")
@RequiredArgsConstructor
public class RecruiterTrainingDashboardController {

    private final CandidateTrainingService candidateTrainingService;

    /**
     * Get training records for all candidates of a specific job
     *
     * @param jobId ID of the job
     * @return ResponseEntity with the result
     */
    @GetMapping("/jobs/{jobId}/training-records")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> getTrainingRecordsByJob(@PathVariable Long jobId) {
        try {
            Response<?> response = candidateTrainingService.getTrainingRecordsByJob(jobId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return handleException(e, "retrieving training records");
        }
    }

    /**
     * Get training status summary for a specific job
     *
     * @param jobId ID of the job
     * @return ResponseEntity with the result
     */
    @GetMapping("/jobs/{jobId}/training-summary")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> getTrainingStatusSummary(@PathVariable Long jobId) {
        try {
            Response<?> response = candidateTrainingService.getTrainingStatusSummary(jobId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return handleException(e, "retrieving training summary");
        }
    }

    /**
     * Record training view for a candidate (for recruiters to record on behalf of candidates)
     *
     * @param jobId ID of the job
     * @param candidateId ID of the candidate
     * @param materialId ID of the training material
     * @return ResponseEntity with the result
     */
    @PostMapping("/jobs/{jobId}/candidates/{candidateId}/training-materials/{materialId}/view")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> recordTrainingView(
            @PathVariable Long jobId,
            @PathVariable Long candidateId,
            @PathVariable Long materialId) {

        try {
            Response<?> response = candidateTrainingService.recordTrainingView(
                    candidateId, jobId, materialId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return handleException(e, "recording training view");
        }
    }

    /**
     * Mark training as completed for a candidate (for recruiters to mark on behalf of candidates)
     *
     * @param jobId ID of the job
     * @param candidateId ID of the candidate
     * @param materialId ID of the training material
     * @return ResponseEntity with the result
     */
    @PostMapping("/jobs/{jobId}/candidates/{candidateId}/training-materials/{materialId}/complete")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> markTrainingCompleted(
            @PathVariable Long jobId,
            @PathVariable Long candidateId,
            @PathVariable Long materialId) {

        try {
            Response<?> response = candidateTrainingService.markTrainingCompleted(
                    candidateId, jobId, materialId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return handleException(e, "marking training completed");
        }
    }

    /**
     * Get specific training record for a candidate
     *
     * @param jobId ID of the job
     * @param candidateId ID of the candidate
     * @param materialId ID of the training material
     * @return ResponseEntity with the result
     */
    @GetMapping("/jobs/{jobId}/candidates/{candidateId}/training-materials/{materialId}/record")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> getTrainingRecord(
            @PathVariable Long jobId,
            @PathVariable Long candidateId,
            @PathVariable Long materialId) {

        try {
            Response<?> response = candidateTrainingService.getTrainingRecord(
                    candidateId, jobId, materialId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return handleException(e, "retrieving training record");
        }
    }

    /**
     * Helper method to handle exceptions consistently across controller methods
     *
     * @param e The exception that occurred
     * @param operation Description of the operation that failed
     * @return Standardized error response
     */
    private ResponseEntity<Response<?>> handleException(Exception e, String operation) {
        return ResponseEntity.status(500).body(new Response<>(500, "Error " + operation + ": " + e.getMessage(), null));
    }
}