package com.event.recruitment.intelligent_recruitment_system.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_selfphotos_comcards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSelfphotoComcard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "comcard_url", nullable = false)
    private String comcardUrl;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    // Constructor for creating a new comcard
    public CandidateSelfphotoComcard(Long candidateId, String comcardUrl) {
        this.candidateId = candidateId;
        this.comcardUrl = comcardUrl;
        this.uploadedAt = LocalDateTime.now();
    }
}