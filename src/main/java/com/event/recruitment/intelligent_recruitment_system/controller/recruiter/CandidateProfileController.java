package com.event.recruitment.intelligent_recruitment_system.controller.recruiter;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateProfileDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateSummaryDTO;
import com.event.recruitment.intelligent_recruitment_system.service.candidate.CandidateProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for recruiters to view candidate profiles
 * Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/controller/recruiter/CandidateProfileController.java
 */
@RestController
@RequestMapping("/api/recruiters/candidates")
@RequiredArgsConstructor
public class CandidateProfileController {

    private final CandidateProfileService candidateProfileService;

    /**
     * Get detailed profile information for a specific candidate
     *
     * @param candidateId The ID of the candidate to view
     * @return ResponseEntity containing the candidate profile data
     */
    @GetMapping("/{candidateId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<CandidateProfileDTO>> getCandidateProfile(@PathVariable Long candidateId) {
        try {
            Response<CandidateProfileDTO> response = candidateProfileService.getCandidateProfile(candidateId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving candidate profile: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Get a paginated list of all candidates with basic information
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Sort field
     * @param direction Sort direction (ASC/DESC)
     * @return ResponseEntity containing a page of candidate summaries
     */
    @GetMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<Page<CandidateSummaryDTO>>> getAllCandidates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        try {
            Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

            Response<Page<CandidateSummaryDTO>> response = candidateProfileService.getAllCandidates(pageable);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving candidates: " + e.getMessage(), null)
            );
        }
    }
}