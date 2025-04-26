package com.event.recruitment.intelligent_recruitment_system.controller.candidate;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import com.event.recruitment.intelligent_recruitment_system.service.candidate.CandidateReputationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/candidates/reputation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
@Slf4j
public class CandidateReputationController {

    private final CandidateReputationService reputationService;
    private final CandidateRepository candidateRepository;
    private final SecurityUtil securityUtil;

    /**
     * Get the current candidate's reputation score
     * @return Response containing reputation score
     */
    @GetMapping("/score")
    public ResponseEntity<Response<?>> getReputationScore() {
        try {
            String username = securityUtil.getCurrentUsername();
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

            if (candidateOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(new Response<>(404, "Candidate not found", null));
            }

            Long candidateId = candidateOpt.get().getId();
            Response<?> response = reputationService.getCandidateReputation(candidateId);

            return ResponseEntity.status(response.getStatusCode())
                    .body(response);
        } catch (Exception e) {
            log.error("Error retrieving reputation score: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(new Response<>(500, "Error retrieving reputation score: " + e.getMessage(), null));
        }
    }

    /**
     * Get the reputation event history for the current candidate
     * @return Response containing reputation events
     */
    @GetMapping("/history")
    public ResponseEntity<Response<?>> getReputationHistory() {
        try {
            String username = securityUtil.getCurrentUsername();
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

            if (candidateOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(new Response<>(404, "Candidate not found", null));
            }

            Long candidateId = candidateOpt.get().getId();
            Response<?> response = reputationService.getCandidateReputationHistory(candidateId);

            return ResponseEntity.status(response.getStatusCode())
                    .body(response);
        } catch (Exception e) {
            log.error("Error retrieving reputation history: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(new Response<>(500, "Error retrieving reputation history: " + e.getMessage(), null));
        }
    }

    /**
     * Get both reputation score and history in a single API call
     * @return Response containing reputation data
     */
    @GetMapping
    public ResponseEntity<Response<?>> getReputationData() {
        try {
            String username = securityUtil.getCurrentUsername();
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

            if (candidateOpt.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(new Response<>(404, "Candidate not found", null));
            }

            Long candidateId = candidateOpt.get().getId();

            // Get both reputation score and history
            Response<?> scoreResponse = reputationService.getCandidateReputation(candidateId);
            Response<?> historyResponse = reputationService.getCandidateReputationHistory(candidateId);

            // Create a combined response data object
            var combinedData = new CombinedReputationData(
                    scoreResponse.getData(),
                    historyResponse.getData()
            );

            return ResponseEntity.ok(new Response<>(200, "Reputation data retrieved successfully", combinedData));
        } catch (Exception e) {
            log.error("Error retrieving reputation data: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(new Response<>(500, "Error retrieving reputation data: " + e.getMessage(), null));
        }
    }

    /**
     * DTO class to combine reputation score and history data
     */
    private record CombinedReputationData(Object score, Object history) {}
}