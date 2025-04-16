// Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/dto/request/training/GenerateQuizRequest.java
package com.event.recruitment.intelligent_recruitment_system.dto.request.training;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateQuizRequest {
    private Long jobId;
    private String trainingMaterialUrl;
    private String jobTitle;
    private String jobDescription;
}