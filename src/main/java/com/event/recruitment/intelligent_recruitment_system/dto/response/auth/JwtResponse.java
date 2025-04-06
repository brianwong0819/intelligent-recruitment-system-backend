package com.event.recruitment.intelligent_recruitment_system.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String jwtToken;
    private String refreshToken;
    private String username;
    private String role;
    private Long id;
    private String email;
}