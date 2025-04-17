// Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/service/auth/AdminAuthService.java

package com.event.recruitment.intelligent_recruitment_system.service.auth;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.auth.AdminLoginRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.auth.AdminJwtResponse;
import com.event.recruitment.intelligent_recruitment_system.model.entity.admin.Admin;
import com.event.recruitment.intelligent_recruitment_system.repository.admin.AdminRepository;
import com.event.recruitment.intelligent_recruitment_system.security.jwt.JwtUtil;
import com.event.recruitment.intelligent_recruitment_system.security.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    // Method to initialize the admin user (call this at application startup)
    public void initializeAdminUser() {
        if (adminRepository.count() == 0) {
            // Create default admin user if none exists
            Admin admin = Admin.builder()
                    .username("admin")
                    .email("admin@system.com")
                    .password(passwordEncoder.encode("Admin123"))
                    .build();

            adminRepository.save(admin);
        }
    }

    public Response<?> login(AdminLoginRequest loginRequest) {
        try {
            Optional<Admin> adminOpt = adminRepository.findByUsername(loginRequest.getUsername());

            if (adminOpt.isEmpty()) {
                return new Response<>(HttpStatus.UNAUTHORIZED.value(), "Invalid username or password", null);
            }

            Admin admin = adminOpt.get();

            // Verify password
            if (!passwordEncoder.matches(loginRequest.getPassword(), admin.getPassword())) {
                return new Response<>(HttpStatus.UNAUTHORIZED.value(), "Invalid username or password", null);
            }

            // Generate tokens
            String jwt = jwtUtil.generateToken(admin.getUsername(), "ADMIN");
            Response<String> refreshTokenResponse = refreshTokenService.createRefreshToken(admin.getUsername());
            String refreshToken = refreshTokenResponse.getData();

            AdminJwtResponse jwtResponse = AdminJwtResponse.builder()
                    .token(jwt)
                    .refreshToken(refreshToken)
                    .username(admin.getUsername())
                    .role("ADMIN")
                    .build();

            return new Response<>(HttpStatus.OK.value(), "Login successful", jwtResponse);
        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error during login: " + e.getMessage(), null);
        }
    }
}