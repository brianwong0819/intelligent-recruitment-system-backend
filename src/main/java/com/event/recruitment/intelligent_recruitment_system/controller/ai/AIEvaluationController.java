package com.event.recruitment.intelligent_recruitment_system.controller.ai;

import com.event.recruitment.intelligent_recruitment_system.dto.ai.AIRatingRequestDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.ai.CandidateAIEvaluationDataDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.service.ai.AIRatingService;
import com.event.recruitment.intelligent_recruitment_system.service.ai.CandidateAIEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIEvaluationController {

    private final CandidateAIEvaluationService candidateAIEvaluationService;
    private final AIRatingService aiRatingService;

    /**
     * Get evaluation data for a job application
     *
     * @param jobApplicationId The ID of the job application
     * @return Response with candidate evaluation data
     */
    @GetMapping("/evaluation-data/application/{jobApplicationId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> getEvaluationData(@PathVariable Long jobApplicationId) {
        Response<?> response = candidateAIEvaluationService.collectCandidateEvaluationData(jobApplicationId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Get evaluation data for a group of job applications
     *
     * @param applicationGroupId The group ID of the job applications
     * @return Response with candidate evaluation data
     */
    @GetMapping("/evaluation-data/group/{applicationGroupId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> getEvaluationDataByGroup(@PathVariable String applicationGroupId) {
        Response<?> response = candidateAIEvaluationService.collectCandidateEvaluationDataByGroupId(applicationGroupId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Save AI rating for a job application
     *
     * @param ratingRequest The AI rating data
     * @return Response indicating success or failure
     */
    @PostMapping("/rating")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> saveAIRating(@RequestBody AIRatingRequestDTO ratingRequest) {
        Response<?> response = aiRatingService.saveAIRating(ratingRequest);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Trigger AI evaluation process for a job application
     *
     * @param jobApplicationId The ID of the job application to evaluate
     * @return Response indicating success or failure
     */
    @PostMapping("/evaluate/application/{jobApplicationId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> evaluateCandidate(@PathVariable Long jobApplicationId) {
        Response<?> response = aiRatingService.evaluateCandidate(jobApplicationId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Trigger AI evaluation process for a group of job applications
     *
     * @param applicationGroupId The group ID of the job applications to evaluate
     * @return Response indicating success or failure
     */
    @PostMapping("/evaluate/group/{applicationGroupId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> evaluateApplicationGroup(@PathVariable String applicationGroupId) {
        Response<?> response = aiRatingService.evaluateByGroupId(applicationGroupId);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}