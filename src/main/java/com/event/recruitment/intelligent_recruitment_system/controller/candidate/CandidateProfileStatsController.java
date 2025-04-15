package com.event.recruitment.intelligent_recruitment_system.controller.candidate;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateProfileStatsDTO;
import com.event.recruitment.intelligent_recruitment_system.service.candidate.CandidateProfileStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
public class CandidateProfileStatsController {

    private final CandidateProfileStatsService candidateProfileStatsService;

    /**
     * Endpoint to get the candidate's profile statistics
     * When no candidateId is provided, it returns statistics for the current logged-in user
     */
    @GetMapping("/profile-stats")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<Response<CandidateProfileStatsDTO>> getProfileStats() {
        try {
            Response<CandidateProfileStatsDTO> response = candidateProfileStatsService.getCandidateProfileStats(null);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error retrieving profile statistics: " + e.getMessage(), null));
        }
    }

    /**
     * Endpoint for recruiters or admins to get profile statistics for a specific candidate
     */
    @GetMapping("/profile-stats/{candidateId}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    public ResponseEntity<Response<CandidateProfileStatsDTO>> getCandidateProfileStats(@PathVariable Long candidateId) {
        try {
            Response<CandidateProfileStatsDTO> response = candidateProfileStatsService.getCandidateProfileStats(candidateId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error retrieving profile statistics: " + e.getMessage(), null));
        }
    }
}