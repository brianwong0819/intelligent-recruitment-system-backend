package com.event.recruitment.intelligent_recruitment_system.model.entity.candidate;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reputation_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReputationEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "job_application_id")
    private Long jobApplicationId;

    @Column(name = "application_group_id")
    private String applicationGroupId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "score_change", nullable = false, precision = 5, scale = 2)
    private BigDecimal scoreChange;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}