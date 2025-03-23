package com.event.recruitment.intelligent_recruitment_system.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table(name = "candidate_experiences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateExperience {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    private JobType jobType;

    @Column(name = "experience_text", nullable = false, columnDefinition = "TEXT")
    private String experienceText;

    // Constructor for creating a new experience
    public CandidateExperience(Long candidateId, JobType jobType, String experienceText) {
        this.candidateId = candidateId;
        this.jobType = jobType;
        this.experienceText = experienceText;
    }
}