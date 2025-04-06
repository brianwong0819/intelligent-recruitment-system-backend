package com.event.recruitment.intelligent_recruitment_system.dto.response.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateSelfphotoComcard;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateComcardDTO {
    private Long id;
    private String comcardUrl;
    private LocalDateTime uploadedAt;

    // Constructor to convert entity to DTO
    public CandidateComcardDTO(CandidateSelfphotoComcard comcard) {
        this.id = comcard.getId();
        this.comcardUrl = comcard.getComcardUrl();
        this.uploadedAt = comcard.getUploadedAt();
    }
}