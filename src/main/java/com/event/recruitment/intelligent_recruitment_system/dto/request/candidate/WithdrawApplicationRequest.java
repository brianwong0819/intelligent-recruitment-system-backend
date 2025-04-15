// Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/dto/request/candidate/WithdrawApplicationRequest.java
package com.event.recruitment.intelligent_recruitment_system.dto.request.candidate;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawApplicationRequest {

    @Size(max = 1000, message = "Withdrawal reason cannot exceed 1000 characters")
    private String withdrawalReason;
}