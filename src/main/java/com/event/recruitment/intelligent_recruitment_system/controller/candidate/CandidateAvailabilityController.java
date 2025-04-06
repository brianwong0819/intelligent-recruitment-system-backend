package com.event.recruitment.intelligent_recruitment_system.controller.candidate;

import com.event.recruitment.intelligent_recruitment_system.dto.request.candidate.UpdateAvailabilityRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.service.candidate.CandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candidates/availability")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_CANDIDATE')")
public class CandidateAvailabilityController {

    private final CandidateService candidateService;

    @PutMapping
    public ResponseEntity<Response<?>> updateAvailability(
            @Valid @RequestBody UpdateAvailabilityRequest updateRequest) {
        try {
            Response<?> response = candidateService.updateAvailability(updateRequest);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error updating availability", null));
        }
    }

    @GetMapping
    public ResponseEntity<Response<?>> getAvailability() {
        try {
            Response<?> response = candidateService.getAvailability();
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error retrieving availability", null));
        }
    }
}