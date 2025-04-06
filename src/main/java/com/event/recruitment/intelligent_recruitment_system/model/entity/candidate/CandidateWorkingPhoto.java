package com.event.recruitment.intelligent_recruitment_system.model.entity.candidate;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_working_photos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateWorkingPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "photo_url", nullable = false)
    private String photoUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    // Constructor for creating a new working photo
    public CandidateWorkingPhoto(Long candidateId, String photoUrl, String description) {
        this.candidateId = candidateId;
        this.photoUrl = photoUrl;
        this.description = description;
        this.uploadedAt = LocalDateTime.now();
    }
}