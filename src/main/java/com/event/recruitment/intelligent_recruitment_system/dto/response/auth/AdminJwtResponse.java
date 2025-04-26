package com.event.recruitment.intelligent_recruitment_system.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminJwtResponse {
    private String token;
    private String refreshToken;
    private String username;
    private String role;
}