package com.event.recruitment.intelligent_recruitment_system.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for the response from the Gemini AI evaluation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiAIResponseDTO {
    private Double experienceScore;
    private Double skillsScore;
    private Double locationScore;
    private Double availabilityScore;
    private Double resumeScore;
    private Double reputationScore;
    private Double aiModelScore;
    private Double finalScore;
    private String feedback;

    // Error information
    private String error;
    private String rawResponse;

    /**
     * Check if the response contains an error
     *
     * @return true if there is an error
     */
    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    /**
     * Check if this is a valid response with all required fields
     *
     * @return true if all required fields are present
     */
    public boolean isValid() {
        return !hasError()
                && experienceScore != null
                && skillsScore != null
                && locationScore != null
                && availabilityScore != null
                && finalScore != null
                && feedback != null;
    }
}