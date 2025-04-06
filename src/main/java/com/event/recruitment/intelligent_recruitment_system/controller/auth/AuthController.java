package com.event.recruitment.intelligent_recruitment_system.controller.auth;

import com.event.recruitment.intelligent_recruitment_system.dto.request.auth.LoginRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Validated
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/candidate/login")
    public ResponseEntity<Response<?>> candidateLogin(@RequestBody @Valid LoginRequest loginRequest) {
        try {
            Response<?> response = authService.candidateLogin(loginRequest);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error logging in candidate: " + e.getMessage(), null));
        }
    }

    @PostMapping("/recruiter/login")
    public ResponseEntity<Response<?>> recruiterLogin(@RequestBody @Valid LoginRequest loginRequest) {
        try {
            Response<?> response = authService.recruiterLogin(loginRequest);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error logging in recruiter: " + e.getMessage(), null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Response<?>> logout(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");

            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.status(400)
                        .body(new Response<>(400, "Refresh token is required", null));
            }

            Response<?> response = authService.logout(refreshToken);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new Response<>(500, "Error processing logout: " + e.getMessage(), null));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Response<?>> refreshAccessToken(@RequestBody @Valid Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body(new Response<>(400, "Refresh token is required", null));
            }
            Response<?> response = authService.refreshAccessToken(refreshToken);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error refreshing access token: " + e.getMessage(), null));
        }
    }
}