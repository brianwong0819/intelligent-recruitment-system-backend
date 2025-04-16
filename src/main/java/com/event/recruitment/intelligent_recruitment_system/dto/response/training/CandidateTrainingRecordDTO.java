// Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/dto/response/training/CandidateTrainingRecordDTO.java

package com.event.recruitment.intelligent_recruitment_system.dto.response.training;

import com.event.recruitment.intelligent_recruitment_system.model.entity.training.CandidateTrainingRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateTrainingRecordDTO {
    private Long id;
    private Long candidateId;
    private String candidateName; // Will be populated separately
    private Long jobId;
    private Long trainingMaterialId;
    private LocalDateTime firstViewedAt;
    private LocalDateTime lastViewedAt;
    private Integer viewCount;
    private Boolean isCompleted;
    private LocalDateTime completionDate;

    public CandidateTrainingRecordDTO(CandidateTrainingRecord record) {
        this.id = record.getId();
        this.candidateId = record.getCandidateId();
        this.jobId = record.getJobId();
        this.trainingMaterialId = record.getTrainingMaterialId();
        this.firstViewedAt = record.getFirstViewedAt();
        this.lastViewedAt = record.getLastViewedAt();
        this.viewCount = record.getViewCount();
        this.isCompleted = record.getIsCompleted();
        this.completionDate = record.getCompletionDate();
    }
}