package com.event.recruitment.intelligent_recruitment_system.controller.candidate;

import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateExperienceDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.request.candidate.CandidateExperienceRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.service.candidate.CandidateExperienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/candidate/experiences")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_CANDIDATE')")
public class CandidateExperienceController {

    private final CandidateExperienceService experienceService;

    /**
     * Add a new experience
     */
    @PostMapping
    public ResponseEntity<Response<?>> addExperience(@Valid @RequestBody CandidateExperienceRequest request) {
        try {
            Response<?> response = experienceService.addExperience(request);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error adding experience: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Get all experiences
     */
    @GetMapping
    public ResponseEntity<Response<?>> getAllExperiences() {
        try {
            Response<?> response = experienceService.getAllExperiences();
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving experiences: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Get experience by ID
     */
    @GetMapping("/{experienceId}")
    public ResponseEntity<Response<?>> getExperienceById(@PathVariable Long experienceId) {
        try {
            Response<?> response = experienceService.getExperienceById(experienceId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving experience: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Update an experience
     */
    @PutMapping("/{experienceId}")
    public ResponseEntity<Response<?>> updateExperience(
            @PathVariable Long experienceId,
            @Valid @RequestBody CandidateExperienceRequest request) {
        try {
            Response<?> response = experienceService.updateExperience(experienceId, request);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error updating experience: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Delete an experience
     */
    @DeleteMapping("/{experienceId}")
    public ResponseEntity<Response<?>> deleteExperience(@PathVariable Long experienceId) {
        try {
            Response<?> response = experienceService.deleteExperience(experienceId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error deleting experience: " + e.getMessage(), null)
            );
        }
    }
}