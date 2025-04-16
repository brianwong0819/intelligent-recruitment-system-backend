// Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/controller/training/CandidateTrainingController.java

package com.event.recruitment.intelligent_recruitment_system.controller.training;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import com.event.recruitment.intelligent_recruitment_system.service.training.CandidateTrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CandidateTrainingController {

    private final CandidateTrainingService candidateTrainingService;
    private final CandidateRepository candidateRepository;
    private final SecurityUtil securityUtil;

    /**
     * Record that a candidate has viewed a training material
     * For candidates to record their own views
     *
     * @param jobId ID of the job
     * @param materialId ID of the training material
     * @return ResponseEntity with the result
     */
    @PostMapping("/candidates/jobs/{jobId}/training-materials/{materialId}/view")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Response<?>> recordTrainingView(
            @PathVariable Long jobId,
            @PathVariable Long materialId) {

        try {
            // Get current logged-in candidate
            String username = securityUtil.getCurrentUsername();
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

            if (candidateOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                        new Response<>(404, "Candidate not found", null));
            }

            Candidates candidate = candidateOpt.get();

            Response<?> response = candidateTrainingService.recordTrainingView(
                    candidate.getId(), jobId, materialId);

            return ResponseEntity.status(response.getStatusCode()).body(response);

        } catch (Exception e) {
            return handleException(e, "recording training view");
        }
    }

    /**
     * Mark a training as completed by a candidate
     * For candidates to mark their own completions
     *
     * @param jobId ID of the job
     * @param materialId ID of the training material
     * @return ResponseEntity with the result
     */
    @PostMapping("/candidates/jobs/{jobId}/training-materials/{materialId}/complete")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Response<?>> markTrainingCompleted(
            @PathVariable Long jobId,
            @PathVariable Long materialId) {

        try {
            // Get current logged-in candidate
            String username = securityUtil.getCurrentUsername();
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

            if (candidateOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                        new Response<>(404, "Candidate not found", null));
            }

            Candidates candidate = candidateOpt.get();

            Response<?> response = candidateTrainingService.markTrainingCompleted(
                    candidate.getId(), jobId, materialId);

            return ResponseEntity.status(response.getStatusCode()).body(response);

        } catch (Exception e) {
            return handleException(e, "marking training completed");
        }
    }

    /**
     * Get training record for the current candidate and a specific training material
     *
     * @param jobId ID of the job
     * @param materialId ID of the training material
     * @return ResponseEntity with the result
     */
    @GetMapping("/candidates/jobs/{jobId}/training-materials/{materialId}/record")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Response<?>> getTrainingRecord(
            @PathVariable Long jobId,
            @PathVariable Long materialId) {

        try {
            // Get current logged-in candidate
            String username = securityUtil.getCurrentUsername();
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

            if (candidateOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                        new Response<>(404, "Candidate not found", null));
            }

            Candidates candidate = candidateOpt.get();

            Response<?> response = candidateTrainingService.getTrainingRecord(
                    candidate.getId(), jobId, materialId);

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

    // Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/controller/training/CandidateTrainingController.java

    /**
     * Get training material for a job and record view
     *
     * @param jobId ID of the job
     * @return ResponseEntity with the result
     */
    @GetMapping("/candidates/jobs/{jobId}/training")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Response<?>> getAndRecordTrainingMaterial(@PathVariable Long jobId) {
        try {
            // Get current logged-in candidate
            String username = securityUtil.getCurrentUsername();
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

            if (candidateOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                        new Response<>(404, "Candidate not found", null));
            }

            Candidates candidate = candidateOpt.get();

            Response<?> response = candidateTrainingService.getAndRecordTrainingMaterial(
                    candidate.getId(), jobId);

            return ResponseEntity.status(response.getStatusCode()).body(response);

        } catch (Exception e) {
            return handleException(e, "retrieving and recording training material");
        }
    }

    /**
     * Mark job training as completed
     *
     * @param jobId ID of the job
     * @return ResponseEntity with the result
     */
    @PostMapping("/candidates/jobs/{jobId}/training/complete")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Response<?>> markJobTrainingCompleted(@PathVariable Long jobId) {
        try {
            // Get current logged-in candidate
            String username = securityUtil.getCurrentUsername();
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

            if (candidateOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                        new Response<>(404, "Candidate not found", null));
            }

            Candidates candidate = candidateOpt.get();

            Response<?> response = candidateTrainingService.markJobTrainingCompleted(
                    candidate.getId(), jobId);

            return ResponseEntity.status(response.getStatusCode()).body(response);

        } catch (Exception e) {
            return handleException(e, "marking training completed");
        }
    }

    /**
     * Get training status for a job
     *
     * @param jobId ID of the job
     * @return ResponseEntity with the result
     */
    @GetMapping("/candidates/jobs/{jobId}/training/status")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Response<?>> getJobTrainingStatus(@PathVariable Long jobId) {
        try {
            // Get current logged-in candidate
            String username = securityUtil.getCurrentUsername();
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

            if (candidateOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                        new Response<>(404, "Candidate not found", null));
            }

            Candidates candidate = candidateOpt.get();

            Response<?> response = candidateTrainingService.getJobTrainingStatus(
                    candidate.getId(), jobId);

            return ResponseEntity.status(response.getStatusCode()).body(response);

        } catch (Exception e) {
            return handleException(e, "retrieving training status");
        }
    }
}