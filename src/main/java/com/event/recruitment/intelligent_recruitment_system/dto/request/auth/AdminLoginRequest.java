// Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/dto/request/auth/AdminLoginRequest.java

package com.event.recruitment.intelligent_recruitment_system.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminLoginRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}