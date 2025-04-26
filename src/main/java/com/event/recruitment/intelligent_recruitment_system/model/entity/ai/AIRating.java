package com.event.recruitment.intelligent_recruitment_system.model.entity.ai;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_ratings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_application_id", nullable = false, unique = true)
    private Long jobApplicationId;

    @Column(name = "application_group_id")
    private String applicationGroupId;

    @Column(name = "experience_score", precision = 5, scale = 2)
    private BigDecimal experienceScore;

    @Column(name = "skills_score", precision = 5, scale = 2)
    private BigDecimal skillsScore;

    @Column(name = "location_score", precision = 5, scale = 2)
    private BigDecimal locationScore;

    @Column(name = "availability_score", precision = 5, scale = 2)
    private BigDecimal availabilityScore;

    @Column(name = "resume_score", precision = 5, scale = 2)
    private BigDecimal resumeScore;

    @Column(name = "reputation_score", precision = 5, scale = 2)
    private BigDecimal reputationScore;

    @Column(name = "ai_model_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal aiModelScore;

    @Column(name = "final_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal finalScore;

    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}