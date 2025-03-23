package com.event.recruitment.intelligent_recruitment_system.controller;

import com.event.recruitment.intelligent_recruitment_system.dto.LoginRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.Response;
import com.event.recruitment.intelligent_recruitment_system.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/candidate/login")
    public ResponseEntity<Response<?>> candidateLogin(@RequestBody @Valid LoginRequest loginRequest) {
        try {
            Response<?> response = authService.candidateLogin(loginRequest);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new Response<>(400, "Error logging in candidate", null));
        }
    }

    @PostMapping("/recruiter/login")
    public ResponseEntity<Response<?>> recruiterLogin(@RequestBody @Valid LoginRequest loginRequest) {
        try {
            Response<?> response = authService.recruiterLogin(loginRequest);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new Response<>(400, "Error logging in recruiter", null));
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

            Response<String> response = authService.logout(refreshToken);

            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new Response<>(500, "Error processing logout", null));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Response<String>> refreshAccessToken(@RequestBody @Valid Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body(new Response<>(400, "Refresh token is required", null));
            }
            Response<String> response = authService.refreshAccessToken(refreshToken);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new Response<>(400, "Error refreshing access token", null));
        }
    }

    // 新增 OAuth2 登录接口
    @PostMapping("/oauth2/login")
    public ResponseEntity<Response<?>> oauth2Login(@RequestBody Map<String, String> request) {
        try {
            // 获取前端传递的参数
            String oauthId = request.get("oauthId");
            String email = request.get("email");
            String role = request.get("role");  // 获取用户角色（candidate 或 recruiter）

            // 调用 AuthService 进行 OAuth 登录或注册
            Response<Map<String, String>> response = authService.oauthLogin(oauthId, email, "GOOGLE", role);
            return ResponseEntity.status(response.getStatusCode()).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(400).body(new Response<>(400, "Error during OAuth2 login", null));
        }
    }
}
