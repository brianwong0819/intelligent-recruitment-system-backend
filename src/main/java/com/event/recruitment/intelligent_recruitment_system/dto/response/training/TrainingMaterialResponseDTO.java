package com.event.recruitment.intelligent_recruitment_system.dto.response.training;

import com.event.recruitment.intelligent_recruitment_system.model.entity.training.TrainingMaterial;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingMaterialResponseDTO {
    private Long id;
    private Long jobId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String description;
    private Boolean isEnabled;
    private LocalDateTime uploadedAt;

    public TrainingMaterialResponseDTO(TrainingMaterial material) {
        this.id = material.getId();
        this.jobId = material.getJobId();
        this.fileName = material.getFileName();
        this.fileUrl = material.getFileUrl();
        this.fileSize = material.getFileSize();
        this.description = material.getDescription();
        this.isEnabled = material.getIsEnabled();
        this.uploadedAt = material.getCreatedAt();
    }
}