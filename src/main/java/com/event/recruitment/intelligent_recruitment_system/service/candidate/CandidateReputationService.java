package com.event.recruitment.intelligent_recruitment_system.service.candidate;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateReputation;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.ReputationEvent;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateReputationRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.ReputationEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateReputationService {

    private final CandidateReputationRepository reputationRepository;
    private final ReputationEventRepository eventRepository;

    // Event types
    public static final String EVENT_HIRED_WITHDRAWAL = "HIRED_WITHDRAWAL";
    public static final String EVENT_NO_SHOW = "NO_SHOW";
    public static final String EVENT_EXCELLENT_PERFORMANCE = "EXCELLENT_PERFORMANCE";
    public static final String EVENT_GOOD_PERFORMANCE = "GOOD_PERFORMANCE";
    public static final String EVENT_POOR_PERFORMANCE = "POOR_PERFORMANCE";

    // Reputation penalties and rewards as BigDecimal
    private static final BigDecimal HIRED_WITHDRAWAL_PENALTY = BigDecimal.valueOf(-10.0);
    private static final BigDecimal NO_SHOW_PENALTY = BigDecimal.valueOf(-5.0);
    private static final BigDecimal EXCELLENT_PERFORMANCE_REWARD = BigDecimal.valueOf(5.0);
    private static final BigDecimal GOOD_PERFORMANCE_REWARD = BigDecimal.valueOf(2.0);
    private static final BigDecimal POOR_PERFORMANCE_PENALTY = BigDecimal.valueOf(-2.0);

    /**
     * Apply a reputation change for a hired withdrawal (single application)
     * @param candidateId The ID of the candidate
     * @param jobApplicationId The ID of the job application
     * @param description The description of the withdrawal
     * @return Response with status and message
     */
    @Transactional
    public Response<?> applyHiredWithdrawalPenalty(Long candidateId, Long jobApplicationId, String description) {
        try {
            // Create the reputation event
            ReputationEvent event = createReputationEvent(
                    candidateId,
                    jobApplicationId,
                    EVENT_HIRED_WITHDRAWAL,
                    HIRED_WITHDRAWAL_PENALTY,
                    description,
                    null  // No group ID for individual applications
            );

            // Update the candidate's reputation score
            updateCandidateReputationScore(candidateId, HIRED_WITHDRAWAL_PENALTY);

            log.info("Applied reputation penalty of {} to candidate ID {} for withdrawing from hired job",
                    HIRED_WITHDRAWAL_PENALTY, candidateId);

            return new Response<>(200, "Reputation penalty applied successfully", event);
        } catch (Exception e) {
            log.error("Error applying reputation penalty: {}", e.getMessage(), e);
            return new Response<>(500, "Error applying reputation penalty: " + e.getMessage(), null);
        }
    }

    /**
     * Apply a reputation change for a hired withdrawal group
     * @param candidateId The ID of the candidate
     * @param applicationGroupId The application group ID
     * @param description The description of the withdrawal
     * @return Response with status and message
     */
    @Transactional
    public Response<?> applyHiredWithdrawalPenaltyForGroup(Long candidateId, String applicationGroupId, String description) {
        try {
            // Check if we've already applied a penalty for this group
            boolean penaltyExists = eventRepository.existsByEventTypeAndApplicationGroupId(
                    EVENT_HIRED_WITHDRAWAL, applicationGroupId);

            // If a penalty already exists for this group, don't apply another one
            if (penaltyExists) {
                log.info("Penalty already applied for group ID: {}", applicationGroupId);
                return new Response<>(200, "Penalty already exists for this application group", null);
            }

            // Create a single reputation event for the group
            ReputationEvent event = createReputationEvent(
                    candidateId,
                    null, // No specific job application ID
                    EVENT_HIRED_WITHDRAWAL,
                    HIRED_WITHDRAWAL_PENALTY,
                    "Withdrew from hired job group: " + description,
                    applicationGroupId  // Include the group ID
            );

            // Update the candidate's reputation score (only once per group)
            updateCandidateReputationScore(candidateId, HIRED_WITHDRAWAL_PENALTY);

            log.info("Applied reputation penalty of {} to candidate ID {} for withdrawing from hired job group {}",
                    HIRED_WITHDRAWAL_PENALTY, candidateId, applicationGroupId);

            return new Response<>(200, "Reputation penalty applied successfully for the application group", event);
        } catch (Exception e) {
            log.error("Error applying reputation penalty for group: {}", e.getMessage(), e);
            return new Response<>(500, "Error applying reputation penalty: " + e.getMessage(), null);
        }
    }

    /**
     * Create a reputation event
     */
    private ReputationEvent createReputationEvent(Long candidateId, Long jobApplicationId,
                                                  String eventType, BigDecimal scoreChange,
                                                  String description, String applicationGroupId) {
        ReputationEvent event = ReputationEvent.builder()
                .candidateId(candidateId)
                .jobApplicationId(jobApplicationId)
                .applicationGroupId(applicationGroupId)
                .eventType(eventType)
                .scoreChange(scoreChange)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();

        return eventRepository.save(event);
    }

    /**
     * Update the candidate's reputation score
     */
    private void updateCandidateReputationScore(Long candidateId, BigDecimal scoreChange) {
        CandidateReputation reputation = reputationRepository.findByCandidateId(candidateId)
                .orElseGet(() -> {
                    // If no reputation record exists, create a new one with the default score (100)
                    CandidateReputation newRep = CandidateReputation.builder()
                            .candidateId(candidateId)
                            .score(100.0)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return reputationRepository.save(newRep);
                });

        // Apply the score change, ensuring the score stays between 0 and 100
        double currentScore = reputation.getScore();
        double newScore = Math.min(100.0, Math.max(0.0, currentScore + scoreChange.doubleValue()));
        reputation.setScore(newScore);
        reputation.setUpdatedAt(LocalDateTime.now());

        reputationRepository.save(reputation);
    }

    /**
     * Get the current reputation of a candidate
     */
    public Response<?> getCandidateReputation(Long candidateId) {
        try {
            CandidateReputation reputation = reputationRepository.findByCandidateId(candidateId)
                    .orElseGet(() -> {
                        // If no reputation record exists, create a new one with the default score (100)
                        CandidateReputation newRep = CandidateReputation.builder()
                                .candidateId(candidateId)
                                .score(100.0)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                        return reputationRepository.save(newRep);
                    });

            return new Response<>(200, "Reputation retrieved successfully", reputation);
        } catch (Exception e) {
            log.error("Error retrieving reputation: {}", e.getMessage(), e);
            return new Response<>(500, "Error retrieving reputation: " + e.getMessage(), null);
        }
    }

    /**
     * Get reputation history for a candidate
     */
    public Response<?> getCandidateReputationHistory(Long candidateId) {
        try {
            var events = eventRepository.findByCandidateIdOrderByCreatedAtDesc(candidateId);
            return new Response<>(200, "Reputation history retrieved successfully", events);
        } catch (Exception e) {
            log.error("Error retrieving reputation history: {}", e.getMessage(), e);
            return new Response<>(500, "Error retrieving reputation history: " + e.getMessage(), null);
        }
    }
}