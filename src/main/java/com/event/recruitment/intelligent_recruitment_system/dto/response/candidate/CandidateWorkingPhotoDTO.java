package com.event.recruitment.intelligent_recruitment_system.dto.response.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateWorkingPhoto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateWorkingPhotoDTO {
    private Long id;
    private String photoUrl;
    private String description;
    private LocalDateTime uploadedAt;

    // Constructor to convert entity to DTO
    public CandidateWorkingPhotoDTO(CandidateWorkingPhoto photo) {
        this.id = photo.getId();
        this.photoUrl = photo.getPhotoUrl();
        this.description = photo.getDescription();
        this.uploadedAt = photo.getUploadedAt();
    }
}