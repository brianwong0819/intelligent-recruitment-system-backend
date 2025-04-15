package com.event.recruitment.intelligent_recruitment_system.controller.ai;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.service.ai.AIRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for AI rating operations
 */
@RestController
@RequestMapping("/api/ai/ratings")
@RequiredArgsConstructor
public class AIRatingController {

    private final AIRatingService aiRatingService;

    /**
     * Evaluate a candidate for a job application using AI
     *
     * @param applicationId The application ID
     * @return Response with evaluation results
     */
    @PostMapping("/applications/{applicationId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> evaluateCandidate(@PathVariable Long applicationId) {
        try {
            Response<?> response = aiRatingService.evaluateCandidate(applicationId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500,
                    "Error evaluating candidate: " + e.getMessage(), null));
        }
    }

    /**
     * Evaluate a group of applications using AI
     *
     * @param groupId The application group ID
     * @return Response with evaluation results
     */
    @PostMapping("/application-groups/{groupId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<?>> evaluateApplicationGroup(@PathVariable String groupId) {
        try {
            Response<?> response = aiRatingService.evaluateByGroupId(groupId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500,
                    "Error evaluating application group: " + e.getMessage(), null));
        }
    }
}