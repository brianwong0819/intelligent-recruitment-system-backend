package com.event.recruitment.intelligent_recruitment_system.dto.response.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.enums.Availability;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A simplified version of candidate data for list views
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSummaryDTO {
    private Long id;
    private String name;
    private String profilePictureUrl;
    private Gender gender;
    private Availability availability;
    private String preferredLocation;
    private List<String> languages = new ArrayList<>();
    private List<String> experienceTags = new ArrayList<>();
    private Double reputationScore;
}