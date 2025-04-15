package com.event.recruitment.intelligent_recruitment_system.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for AI rating request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRatingRequestDTO {
    private Long jobApplicationId;
    private String applicationGroupId;
    private Double experienceScore;
    private Double skillsScore;
    private Double locationScore;
    private Double availabilityScore;
    private Double resumeScore;
    private Double reputationScore;
    private Double aiModelScore;
    private Double finalScore;
    private String aiFeedback;

    /**
     * Create from GeminiAIResponseDTO
     *
     * @param jobApplicationId The job application ID
     * @param response The Gemini AI response
     * @return A new AIRatingRequestDTO
     */
    public static AIRatingRequestDTO fromGeminiResponse(Long jobApplicationId,  String applicationGroupId, GeminiAIResponseDTO response) {
        return AIRatingRequestDTO.builder()
                .jobApplicationId(jobApplicationId)
                .applicationGroupId(applicationGroupId)
                .experienceScore(response.getExperienceScore())
                .skillsScore(response.getSkillsScore())
                .locationScore(response.getLocationScore())
                .availabilityScore(response.getAvailabilityScore())
                .resumeScore(response.getResumeScore())
                .reputationScore(response.getReputationScore())
                .aiModelScore(response.getAiModelScore())
                .finalScore(response.getFinalScore())
                .aiFeedback(response.getFeedback())
                .build();
    }
}