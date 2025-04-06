// src/main/java/com/event/recruitment/intelligent_recruitment_system/dto/request/common/ChangePasswordRequest.java
package com.event.recruitment.intelligent_recruitment_system.dto.request.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
}