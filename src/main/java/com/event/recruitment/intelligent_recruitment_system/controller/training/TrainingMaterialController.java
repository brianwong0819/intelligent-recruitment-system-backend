package com.event.recruitment.intelligent_recruitment_system.controller.training;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.training.UpdateTrainingStatusRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.training.UploadTrainingMaterialRequest;
import com.event.recruitment.intelligent_recruitment_system.service.training.TrainingMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TrainingMaterialController {

    private final TrainingMaterialService trainingMaterialService;

    /**
     * Upload a training material for a job
     *
     * @param jobId ID of the job to upload material for
     * @param file The training material file (PDF)
     * @param description Training material description
     * @return ResponseEntity with the result
     */
    @PostMapping("/recruiters/jobs/{jobId}/training-materials")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> uploadTrainingMaterial(
            @PathVariable Long jobId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {

        UploadTrainingMaterialRequest request = new UploadTrainingMaterialRequest(description);
        try {
            Response<?> response = trainingMaterialService.uploadTrainingMaterial(jobId, file, request);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return handleException(e, "uploading training material");
        }
    }

    /**
     * Get all training materials for a job
     *
     * @param jobId ID of the job to get materials for
     * @return ResponseEntity with the result
     */
    @GetMapping("/recruiters/jobs/{jobId}/training-materials")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> getTrainingMaterialsByJob(@PathVariable Long jobId) {
        try {
            Response<?> response = trainingMaterialService.getTrainingMaterialsByJob(jobId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return handleException(e, "retrieving training materials");
        }
    }

    /**
     * Download a specific training material
     *
     * @param materialId ID of the material to download
     * @return ResponseEntity with the file
     */
    @GetMapping("/training/materials/{materialId}")
    public ResponseEntity<?> downloadTrainingMaterial(@PathVariable Long materialId) {
        return trainingMaterialService.downloadTrainingMaterial(materialId);
    }

    /**
     * Delete a training material
     *
     * @param jobId ID of the job the material belongs to
     * @param materialId ID of the material to delete
     * @return ResponseEntity with the result
     */
    @DeleteMapping("/recruiters/jobs/{jobId}/training-materials/{materialId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> deleteTrainingMaterial(
            @PathVariable Long jobId,
            @PathVariable Long materialId) {

        try {
            Response<?> response = trainingMaterialService.deleteTrainingMaterial(jobId, materialId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return handleException(e, "deleting training material");
        }
    }

    /**
     * Update a training material's description
     *
     * @param jobId ID of the job the material belongs to
     * @param materialId ID of the material to update
     * @param request Updated details
     * @return ResponseEntity with the result
     */
    @PutMapping("/recruiters/jobs/{jobId}/training-materials/{materialId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> updateTrainingMaterial(
            @PathVariable Long jobId,
            @PathVariable Long materialId,
            @RequestBody UploadTrainingMaterialRequest request) {

        try {
            Response<?> response = trainingMaterialService.updateTrainingMaterial(jobId, materialId, request);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return handleException(e, "updating training material");
        }
    }

    /**
     * Enable or disable a training material
     *
     * @param jobId ID of the job the material belongs to
     * @param materialId ID of the material to update
     * @param request Contains isEnabled flag
     * @return ResponseEntity with the result
     */
    @PutMapping("/recruiters/jobs/{jobId}/training-materials/{materialId}/status")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> updateTrainingStatus(
            @PathVariable Long jobId,
            @PathVariable Long materialId,
            @RequestBody UpdateTrainingStatusRequest request) {

        try {
            Response<?> response = trainingMaterialService.updateTrainingStatus(jobId, materialId, request);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return handleException(e, "updating training status");
        }
    }

    /**
     * Get training materials for hired candidates
     * Only candidates who have been hired for a job can access its training materials
     *
     * @param jobId ID of the job to get materials for
     * @return ResponseEntity with the result
     */
    @GetMapping("/candidates/jobs/{jobId}/training-materials")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Response<?>> getCandidateTrainingMaterials(@PathVariable Long jobId) {
        try {
            Response<?> response = trainingMaterialService.getEnabledTrainingMaterialsByJob(jobId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return handleException(e, "retrieving training materials");
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