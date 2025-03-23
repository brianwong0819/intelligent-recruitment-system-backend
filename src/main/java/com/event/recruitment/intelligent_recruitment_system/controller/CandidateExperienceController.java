package com.event.recruitment.intelligent_recruitment_system.controller;

import com.event.recruitment.intelligent_recruitment_system.dto.CandidateExperienceDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.CandidateExperienceRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.Response;
import com.event.recruitment.intelligent_recruitment_system.service.CandidateExperienceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/candidate/experiences")
public class CandidateExperienceController {

    @Autowired
    private CandidateExperienceService experienceService;

    /**
     * Add a new experience
     */
    @PostMapping
    public ResponseEntity<Response<CandidateExperienceDTO>> addExperience(
            @Valid @RequestBody CandidateExperienceRequest request) {
        try {
            Response<CandidateExperienceDTO> response = experienceService.addExperience(request);
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
    public ResponseEntity<Response<List<CandidateExperienceDTO>>> getAllExperiences() {
        try {
            Response<List<CandidateExperienceDTO>> response = experienceService.getAllExperiences();
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
    public ResponseEntity<Response<CandidateExperienceDTO>> getExperienceById(
            @PathVariable Long experienceId) {
        try {
            Response<CandidateExperienceDTO> response = experienceService.getExperienceById(experienceId);
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
    public ResponseEntity<Response<CandidateExperienceDTO>> updateExperience(
            @PathVariable Long experienceId,
            @Valid @RequestBody CandidateExperienceRequest request) {
        try {
            Response<CandidateExperienceDTO> response = experienceService.updateExperience(experienceId, request);
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
    public ResponseEntity<Response<Void>> deleteExperience(@PathVariable Long experienceId) {
        try {
            Response<Void> response = experienceService.deleteExperience(experienceId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error deleting experience: " + e.getMessage(), null)
            );
        }
    }
}