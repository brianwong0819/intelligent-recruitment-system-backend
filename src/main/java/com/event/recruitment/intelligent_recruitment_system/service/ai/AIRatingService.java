package com.event.recruitment.intelligent_recruitment_system.service.ai;

import com.event.recruitment.intelligent_recruitment_system.dto.ai.AIRatingRequestDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.ai.CandidateAIEvaluationDataDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.ai.GeminiAIResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.model.entity.ai.AIRating;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobApplication;
import com.event.recruitment.intelligent_recruitment_system.repository.ai.AIRatingRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIRatingService {

    private final AIRatingRepository aiRatingRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final CandidateAIEvaluationService evaluationDataService;
    private final GeminiAIService geminiAIService;

    /**
     * Evaluates a candidate application using AI by application group ID
     *
     * @param applicationGroupId Group ID of applications to evaluate
     * @return Response with evaluation results
     */
    @Transactional
    public Response<?> evaluateByGroupId(String applicationGroupId) {
        try {
            // 1. Collect all necessary data for evaluation
            Response<CandidateAIEvaluationDataDTO> dataResponse =
                    evaluationDataService.collectCandidateEvaluationDataByGroupId(applicationGroupId);

            if (dataResponse.getStatusCode() != 200 || dataResponse.getData() == null) {
                return new Response<>(dataResponse.getStatusCode(),
                        "Failed to collect evaluation data: " + dataResponse.getMessage(), null);
            }

            CandidateAIEvaluationDataDTO evaluationData = dataResponse.getData();

            // Get the first application ID from the group
            if (evaluationData.getJobApplicationIds() == null || evaluationData.getJobApplicationIds().isEmpty()) {
                return new Response<>(404, "No job applications found in this group", null);
            }

            Long jobApplicationId = evaluationData.getJobApplicationIds().get(0);

            // 2. Process the data with Gemini AI
            GeminiAIResponseDTO aiResponse = geminiAIService.evaluateCandidate(evaluationData);

            if (aiResponse.hasError()) {
                log.error("Gemini AI evaluation failed: {}", aiResponse.getError());
                // Use default rating if AI evaluation fails
                AIRatingRequestDTO fallbackRating = createFallbackRating(evaluationData, jobApplicationId);
                fallbackRating.setAiFeedback(generateFallbackFeedback(evaluationData));

                // Save the rating
                Response<?> saveResponse = saveAIRating(fallbackRating);

                if (saveResponse.getStatusCode() != 200) {
                    return new Response<>(saveResponse.getStatusCode(),
                            "Failed to save fallback AI rating: " + saveResponse.getMessage(), null);
                }

                return new Response<>(200,
                        "Candidate evaluated with fallback mechanism due to AI error: " + aiResponse.getError(),
                        saveResponse.getData());
            }

            // Create rating from AI response for the first application only
            AIRatingRequestDTO ratingRequest = AIRatingRequestDTO.fromGeminiResponse(jobApplicationId, aiResponse);

            // Override with calculated values
            updateRatingWithCalculatedValues(ratingRequest, evaluationData);

            // Save the AI rating
            Response<?> saveResponse = saveAIRating(ratingRequest);

            if (saveResponse.getStatusCode() != 200) {
                return new Response<>(saveResponse.getStatusCode(),
                        "Failed to save AI rating: " + saveResponse.getMessage(), null);
            }

            return new Response<>(200, "Candidate application evaluated successfully", saveResponse.getData());

        } catch (Exception e) {
            log.error("Error evaluating candidate group: {}", e.getMessage(), e);
            return new Response<>(500, "Error evaluating candidate group: " + e.getMessage(), null);
        }
    }

    /**
     * Evaluates a candidate application using AI by job application ID
     *
     * @param jobApplicationId ID of the job application to evaluate
     * @return Response with evaluation results
     */
    @Transactional
    public Response<?> evaluateCandidate(Long jobApplicationId) {
        try {
            // Get the application
            Optional<JobApplication> applicationOpt = jobApplicationRepository.findById(jobApplicationId);

            if (applicationOpt.isEmpty()) {
                return new Response<>(404, "Job application not found", null);
            }

            JobApplication application = applicationOpt.get();

            // Check if application is part of a group
            if (application.getApplicationGroupId() != null) {
                // If it's part of a group, evaluate the whole group
                return evaluateByGroupId(application.getApplicationGroupId());
            }

            // If it's a single application, evaluate it individually
            Response<CandidateAIEvaluationDataDTO> dataResponse =
                    evaluationDataService.collectCandidateEvaluationData(jobApplicationId);

            if (dataResponse.getStatusCode() != 200 || dataResponse.getData() == null) {
                return new Response<>(dataResponse.getStatusCode(),
                        "Failed to collect evaluation data: " + dataResponse.getMessage(), null);
            }

            CandidateAIEvaluationDataDTO evaluationData = dataResponse.getData();

            // Get AI evaluation
            GeminiAIResponseDTO aiResponse = geminiAIService.evaluateCandidate(evaluationData);

            // Create rating request from AI response
            AIRatingRequestDTO ratingRequest;

            if (aiResponse.hasError()) {
                log.error("Gemini AI evaluation failed: {}", aiResponse.getError());
                // Use fallback rating if AI evaluation fails
                ratingRequest = createFallbackRating(evaluationData, jobApplicationId);
            } else {
                ratingRequest = AIRatingRequestDTO.fromGeminiResponse(jobApplicationId, aiResponse);
                // Override with calculated values
                updateRatingWithCalculatedValues(ratingRequest, evaluationData);
            }

            // Save the AI rating results
            Response<?> saveResponse = saveAIRating(ratingRequest);

            if (saveResponse.getStatusCode() != 200) {
                return new Response<>(saveResponse.getStatusCode(),
                        "Failed to save AI rating: " + saveResponse.getMessage(), null);
            }

            return new Response<>(200, "Candidate evaluated successfully", saveResponse.getData());

        } catch (Exception e) {
            log.error("Error evaluating candidate: {}", e.getMessage(), e);
            return new Response<>(500, "Error evaluating candidate: " + e.getMessage(), null);
        }
    }

    /**
     * Process the evaluation for a group of applications using Gemini AI results
     *
     * @param evaluationData The collected evaluation data
     * @param aiResponse The Gemini AI response
     * @return List of saved AI ratings
     */
    private List<AIRating> processGroupEvaluationWithAI(
            CandidateAIEvaluationDataDTO evaluationData,
            GeminiAIResponseDTO aiResponse) {

        // Use AI feedback for all applications
        String feedback = aiResponse.getFeedback();

        return evaluationData.getJobApplicationIds().stream()
                .map(appId -> {
                    // Create a rating for this application using AI response
                    AIRatingRequestDTO rating = AIRatingRequestDTO.fromGeminiResponse(appId, aiResponse);

                    // Override with calculated values
                    updateRatingWithCalculatedValues(rating, evaluationData);

                    // Save the rating
                    Response<?> saveResponse = saveAIRating(rating);

                    if (saveResponse.getStatusCode() == 200 && saveResponse.getData() != null) {
                        return (AIRating) saveResponse.getData();
                    } else {
                        log.error("Failed to save rating for application ID {}: {}",
                                appId, saveResponse.getMessage());
                        return null;
                    }
                })
                .filter(rating -> rating != null)
                .collect(Collectors.toList());
    }

    /**
     * Update rating request with calculated values instead of AI-provided values
     *
     * @param ratingRequest The rating request to update
     * @param evaluationData The evaluation data
     */
    private void updateRatingWithCalculatedValues(
            AIRatingRequestDTO ratingRequest,
            CandidateAIEvaluationDataDTO evaluationData) {

        // 1. Use distanceToCandidate directly as locationScore
        if (evaluationData.getDistanceToCandidate() != null) {
            ratingRequest.setLocationScore(evaluationData.getDistanceToCandidate());
        }

        // 2. Calculate availability score based on work days ratio
        double availabilityScore = calculateAvailabilityScore(evaluationData);
        ratingRequest.setAvailabilityScore(availabilityScore);

        // 3. Use reputation score directly from data
        if (evaluationData.getReputationScore() != null) {
            ratingRequest.setReputationScore(evaluationData.getReputationScore());
        }

        // 4. Calculate final score as aiModelScore + reputationScore
        double finalScore = ratingRequest.getAiModelScore() +
                (ratingRequest.getReputationScore() != null ? ratingRequest.getReputationScore() : 0);
        ratingRequest.setFinalScore(finalScore);
    }

    /**
     * Calculate availability score based on work days ratio and job requirements
     *
     * @param evaluationData The evaluation data
     * @return The calculated availability score
     */
    private double calculateAvailabilityScore(CandidateAIEvaluationDataDTO evaluationData) {
        double availabilityScore = 5.0; // Default score

        // Calculate based on ratio of totalWorkDays/totalJobWorkingDays
        if (evaluationData.getTotalJobWorkingDays() != null && evaluationData.getTotalJobWorkingDays() > 0
                && evaluationData.getTotalWorkDays() != null) {
            double ratio = (double) evaluationData.getTotalWorkDays() / evaluationData.getTotalJobWorkingDays();
            availabilityScore = Math.min(10.0, ratio * 10.0); // Scale to 10
        }

        // Check job requirements for commitment mentions
        String jobRequirements = evaluationData.getJobRequirements();
        if (jobRequirements != null &&
                (jobRequirements.toLowerCase().contains("commit") ||
                        jobRequirements.toLowerCase().contains("full") ||
                        jobRequirements.toLowerCase().contains("all days"))) {
            // Increase weight for commitment requirements
            if (availabilityScore >= 8.0) {
                availabilityScore = 10.0; // Full points for high availability with commitment requirement
            } else {
                availabilityScore += 1.0; // Add bonus point for partial availability
            }
        }

        return Math.min(10.0, Math.max(1.0, availabilityScore)); // Ensure score is between 1-10
    }

    /**
     * Process fallback ratings for a group of applications when AI evaluation fails
     *
     * @param evaluationData The evaluation data
     * @return List of saved AI ratings
     */
    private List<AIRating> processFallbackGroupEvaluation(CandidateAIEvaluationDataDTO evaluationData) {
        // Generate common fallback feedback
        String groupFeedback = generateFallbackFeedback(evaluationData);

        return evaluationData.getJobApplicationIds().stream()
                .map(appId -> {
                    // Create a fallback rating for this application
                    AIRatingRequestDTO rating = createFallbackRating(evaluationData, appId);
                    rating.setAiFeedback(groupFeedback);

                    // Save the rating
                    Response<?> saveResponse = saveAIRating(rating);

                    if (saveResponse.getStatusCode() == 200 && saveResponse.getData() != null) {
                        return (AIRating) saveResponse.getData();
                    } else {
                        log.error("Failed to save fallback rating for application ID {}: {}",
                                appId, saveResponse.getMessage());
                        return null;
                    }
                })
                .filter(rating -> rating != null)
                .collect(Collectors.toList());
    }

    /**
     * Generate fallback feedback when AI evaluation fails
     *
     * @param evaluationData The evaluation data
     * @return Generated feedback
     */
    private String generateFallbackFeedback(CandidateAIEvaluationDataDTO evaluationData) {
        StringBuilder feedback = new StringBuilder();
        feedback.append("Automated Application Assessment (System Generated):\n\n");

        feedback.append("Candidate ").append(evaluationData.getCandidateName())
                .append(" applied for ").append(evaluationData.getJobTitle())
                .append(" at ").append(String.join(", ", evaluationData.getLocationNames()))
                .append(" for ").append(evaluationData.getTotalWorkDays())
                .append(" work day(s) (").append(String.join(", ", evaluationData.getAppliedWorkDates()))
                .append(").\n\n");

        // Add experience assessment
        feedback.append("Experience Assessment: The candidate has ");
        if (evaluationData.getExperiences() != null && !evaluationData.getExperiences().isEmpty()) {
            feedback.append("relevant experience in ")
                    .append(evaluationData.getExperiences().size())
                    .append(" area(s), which aligns with the job requirements.");
        } else {
            feedback.append("no documented experience in our system, which makes it difficult to assess their suitability.");
        }
        feedback.append("\n\n");

        // Add location assessment
        feedback.append("Location Assessment: ");
        if (evaluationData.getDistanceToCandidate() != null) {
            double distance = evaluationData.getDistanceToCandidate();
            if (distance < 5) {
                feedback.append("The job locations are very close to the candidate's preferred location (")
                        .append(String.format("%.2f", distance))
                        .append(" km), which is excellent.");
            } else if (distance < 10) {
                feedback.append("The job locations are reasonably close to the candidate's preferred location (")
                        .append(String.format("%.2f", distance))
                        .append(" km).");
            } else {
                feedback.append("The job locations are quite far from the candidate's preferred location (")
                        .append(String.format("%.2f", distance))
                        .append(" km), which might affect attendance.");
            }
        } else {
            feedback.append("Distance information is not available, so location compatibility cannot be assessed.");
        }
        feedback.append("\n\n");

        // Add availability assessment
        feedback.append("Availability Assessment: ");
        if (evaluationData.getAvailabilityType() != null) {
            String availabilityType = evaluationData.getAvailabilityType();
            if (availabilityType.equals("ANYTIME") || availabilityType.equals("WEEKDAYS") ||
                    availabilityType.equals("WEEKENDS")) {
                feedback.append("The candidate's availability (").append(availabilityType)
                        .append(") should accommodate the job schedule.");
            } else if (availabilityType.equals("CUSTOM_DATES")) {
                feedback.append("The candidate has specified custom availability dates. ");
                if (evaluationData.getAvailableDates() != null && !evaluationData.getAvailableDates().isEmpty()) {
                    // Count matching dates
                    long matchingDates = evaluationData.getAppliedWorkDates().stream()
                            .filter(workDate -> evaluationData.getAvailableDates().contains(workDate))
                            .count();

                    double matchPercentage = (double) matchingDates / evaluationData.getAppliedWorkDates().size() * 100;

                    if (matchPercentage >= 100) {
                        feedback.append("All requested work dates match their availability.");
                    } else if (matchPercentage >= 75) {
                        feedback.append("Most (").append(String.format("%.1f", matchPercentage))
                                .append("%) of the requested work dates match their availability.");
                    } else if (matchPercentage >= 50) {
                        feedback.append("About half (").append(String.format("%.1f", matchPercentage))
                                .append("%) of the requested work dates match their availability.");
                    } else {
                        feedback.append("Only ").append(String.format("%.1f", matchPercentage))
                                .append("% of the requested work dates match their availability.");
                    }
                } else {
                    feedback.append("However, no specific available dates are provided.");
                }
            }
        } else {
            feedback.append("No availability information provided, which makes scheduling assessment difficult.");
        }

        return feedback.toString();
    }

    /**
     * Create a fallback rating for an application when AI evaluation fails
     *
     * @param evaluationData The evaluation data
     * @param applicationId The application ID
     * @return A fallback rating
     */
    private AIRatingRequestDTO createFallbackRating(CandidateAIEvaluationDataDTO evaluationData, Long applicationId) {
        // Calculate scores based on available data
        double experienceScore = evaluationData.getExperiences() != null && !evaluationData.getExperiences().isEmpty() ? 7.5 : 5.0;

        // Use distance directly as location score
        double locationScore = evaluationData.getDistanceToCandidate() != null ?
                evaluationData.getDistanceToCandidate() : 10.0;

        // Calculate availability score
        double availabilityScore = calculateAvailabilityScore(evaluationData);

        // Get AI model score
        double aiModelScore = (experienceScore + 7.0 + 6.5) / 3.0; // Average of experience, skills (7.0) and resume (6.5)
        aiModelScore = Math.min(10.0, Math.max(1.0, aiModelScore)); // Ensure score is between 1-10

        // Use reputation score from data
        double reputationScore = evaluationData.getReputationScore() != null ?
                evaluationData.getReputationScore() : 100.0;

        // Calculate final score
        double finalScore = aiModelScore + reputationScore;

        String feedback = "System generated assessment for job application ID " + applicationId +
                " (AI evaluation service unavailable)";

        return AIRatingRequestDTO.builder()
                .jobApplicationId(applicationId)
                .experienceScore(experienceScore)
                .skillsScore(7.0)     // Default score
                .locationScore(locationScore)
                .availabilityScore(availabilityScore)
                .resumeScore(6.5)     // Default score for resume
                .reputationScore(reputationScore) // Use value from data
                .aiModelScore(aiModelScore)    // Average of available scores
                .finalScore(finalScore)      // Combined score
                .aiFeedback(feedback)
                .build();
    }

    /**
     * Saves AI rating data for a job application
     *
     * @param ratingRequest DTO containing AI rating data
     * @return Response containing the saved rating
     */
    @Transactional
    public Response<?> saveAIRating(AIRatingRequestDTO ratingRequest) {
        try {
            // Check if job application exists
            Optional<JobApplication> jobApplicationOpt = jobApplicationRepository.findById(
                    ratingRequest.getJobApplicationId());

            if (jobApplicationOpt.isEmpty()) {
                return new Response<>(404, "Job application not found", null);
            }

            // Check if rating already exists
            Optional<AIRating> existingRating = aiRatingRepository.findByJobApplicationId(
                    ratingRequest.getJobApplicationId());

            AIRating aiRating;

            if (existingRating.isPresent()) {
                // Update existing rating
                aiRating = existingRating.get();
                if (ratingRequest.getExperienceScore() != null) {
                    aiRating.setExperienceScore(new BigDecimal(ratingRequest.getExperienceScore()));
                }
                if (ratingRequest.getSkillsScore() != null) {
                    aiRating.setSkillsScore(new BigDecimal(ratingRequest.getSkillsScore()));
                }
                if (ratingRequest.getLocationScore() != null) {
                    aiRating.setLocationScore(new BigDecimal(ratingRequest.getLocationScore()));
                }
                if (ratingRequest.getAvailabilityScore() != null) {
                    aiRating.setAvailabilityScore(new BigDecimal(ratingRequest.getAvailabilityScore()));
                }
                if (ratingRequest.getResumeScore() != null) {
                    aiRating.setResumeScore(new BigDecimal(ratingRequest.getResumeScore()));
                }
                if (ratingRequest.getReputationScore() != null) {
                    aiRating.setReputationScore(new BigDecimal(ratingRequest.getReputationScore()));
                }
                aiRating.setAiModelScore(new BigDecimal(ratingRequest.getAiModelScore()));
                aiRating.setFinalScore(new BigDecimal(ratingRequest.getFinalScore()));
                aiRating.setAiFeedback(ratingRequest.getAiFeedback());
                aiRating.setUpdatedAt(LocalDateTime.now());
            } else {
                // Create new rating
                aiRating = AIRating.builder()
                        .jobApplicationId(ratingRequest.getJobApplicationId())
                        .experienceScore(ratingRequest.getExperienceScore() != null ?
                                new BigDecimal(ratingRequest.getExperienceScore()) : null)
                        .skillsScore(ratingRequest.getSkillsScore() != null ?
                                new BigDecimal(ratingRequest.getSkillsScore()) : null)
                        .locationScore(ratingRequest.getLocationScore() != null ?
                                new BigDecimal(ratingRequest.getLocationScore()) : null)
                        .availabilityScore(ratingRequest.getAvailabilityScore() != null ?
                                new BigDecimal(ratingRequest.getAvailabilityScore()) : null)
                        .resumeScore(ratingRequest.getResumeScore() != null ?
                                new BigDecimal(ratingRequest.getResumeScore()) : null)
                        .reputationScore(ratingRequest.getReputationScore() != null ?
                                new BigDecimal(ratingRequest.getReputationScore()) : null)
                        .aiModelScore(new BigDecimal(ratingRequest.getAiModelScore()))
                        .finalScore(new BigDecimal(ratingRequest.getFinalScore()))
                        .aiFeedback(ratingRequest.getAiFeedback())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
            }

            AIRating savedRating = aiRatingRepository.save(aiRating);

            return new Response<>(200, "AI rating saved successfully", savedRating);

        } catch (Exception e) {
            log.error("Error saving AI rating: {}", e.getMessage(), e);
            return new Response<>(500, "Error saving AI rating: " + e.getMessage(), null);
        }
    }
}