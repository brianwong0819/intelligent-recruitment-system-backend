package com.event.recruitment.intelligent_recruitment_system.controller.candidate;

import com.event.recruitment.intelligent_recruitment_system.dto.request.auth.CandidateRegistrationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.service.candidate.CandidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/candidate")
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
}
