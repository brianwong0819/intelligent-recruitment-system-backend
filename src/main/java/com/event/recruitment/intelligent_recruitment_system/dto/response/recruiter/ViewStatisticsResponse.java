// src/main/java/com/event/recruitment/intelligent_recruitment_system/dto/response/recruiter/ViewStatisticsResponse.java

package com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViewStatisticsResponse {
    private Long jobId;
    private Long totalViewers;
    private List<ViewerDetail> recentViewers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViewerDetail {
        private Long candidateId;
        private String candidateName;
        private String viewedAt;
        private Integer viewCount;
    }
}