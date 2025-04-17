package com.event.recruitment.intelligent_recruitment_system.controller.candidate;

import com.event.recruitment.intelligent_recruitment_system.dto.request.auth.CandidateRegistrationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.candidate.UpdateSearchableStatusRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.service.candidate.CandidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/candidates") // Changed from /api/candidate to /api/candidates
@Validated
public class CandidateController {

    private final CandidateService candidateService;

    @Autowired
    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @PostMapping("/register")
    public ResponseEntity<Response<?>> registerCandidate(@RequestBody @Valid CandidateRegistrationRequest candidateRegistrationRequest) {
        try {
            Response<?> response = candidateService.registerCandidate(candidateRegistrationRequest);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new Response<>(400, "Error registering candidate", null));
        }
    }

    @PutMapping("/{id}/update")
    @PreAuthorize("hasRole('ROLE_CANDIDATE')")
    public ResponseEntity<Response<?>> updateCandidate(@PathVariable("id") Long id,
                                                       @RequestBody @Valid CandidateRegistrationRequest candidateRegistrationRequest) {
        try {
            Response<?> response = candidateService.updateCandidate(id, candidateRegistrationRequest);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new Response<>(400, "Error updating candidate", null));
        }
    }

    /**
     * Update candidate searchable status
     * @param request The request to update searchable status
     * @return Response with updated status
     */
    @PutMapping("/searchable")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Response<?>> updateSearchableStatus(@Valid @RequestBody UpdateSearchableStatusRequest request) {
        try {
            Response<?> response = candidateService.updateSearchableStatus(request);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error updating searchable status: " + e.getMessage(), null));
        }
    }

    /**
     * Get current searchable status
     * @return Response with current status
     */
    @GetMapping("/searchable")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Response<?>> getSearchableStatus() {
        try {
            Response<?> response = candidateService.getSearchableStatus();
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error retrieving searchable status: " + e.getMessage(), null));
        }
    }
}