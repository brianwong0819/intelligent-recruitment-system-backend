package com.event.recruitment.intelligent_recruitment_system.service.auth;

import com.event.recruitment.intelligent_recruitment_system.dto.request.auth.LoginRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.response.auth.JwtResponse;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Recruiters;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.recruiter.RecruiterRepository;
import com.event.recruitment.intelligent_recruitment_system.security.jwt.JwtUtil;
import com.event.recruitment.intelligent_recruitment_system.security.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CandidateRepository candidateRepository;
    private final RecruiterRepository recruiterRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    // Candidate login with username/email and password
    public Response<?> candidateLogin(LoginRequest loginRequest) {
        return login(loginRequest, "CANDIDATE");
    }

    // Recruiter login with username/email and password
    public Response<?> recruiterLogin(LoginRequest loginRequest) {
        return login(loginRequest, "RECRUITER");
    }

    // Check if a user exists with a given email
    public Response<?> checkEmailExists(String email) {
        boolean candidateExists = candidateRepository.findByEmail(email).isPresent();
        boolean recruiterExists = recruiterRepository.findByEmail(email).isPresent();

        if (candidateExists || recruiterExists) {
            String role = candidateExists ? "CANDIDATE" : "RECRUITER";
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("exists", true);
            resultMap.put("role", role);
            return new Response<>(200, "User exists", resultMap);
        } else {
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("exists", false);
            return new Response<>(200, "User does not exist", resultMap);
        }
    }

    // Generic login method
    private Response<?> login(LoginRequest loginRequest, String role) {
        String usernameOrEmail = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        // Determine if input is email or username
        boolean isEmail = usernameOrEmail.contains("@");

        Optional<?> userEntityOptional;

        // Check if user exists by username or email
        if (role.equals("CANDIDATE")) {
            userEntityOptional = isEmail
                    ? candidateRepository.findByEmail(usernameOrEmail)
                    : candidateRepository.findByUsername(usernameOrEmail);
        } else {
            userEntityOptional = isEmail
                    ? recruiterRepository.findByEmail(usernameOrEmail)
                    : recruiterRepository.findByUsername(usernameOrEmail);
        }

        if (userEntityOptional.isPresent()) {
            if (role.equals("CANDIDATE")) {
                Candidates candidateEntity = (Candidates) userEntityOptional.get();

                // Verify password
                if (passwordEncoder.matches(password, candidateEntity.getPassword())) {
                    return generateTokensAndReturnResponse(candidateEntity.getUsername(), "CANDIDATE",
                            candidateEntity.getId(), candidateEntity.getEmail());
                } else {
                    return new Response<>(401, "Invalid username/email or password", null);
                }
            } else {
                Recruiters recruiterEntity = (Recruiters) userEntityOptional.get();

                // Verify password
                if (passwordEncoder.matches(password, recruiterEntity.getPassword())) {
                    return generateTokensAndReturnResponse(recruiterEntity.getUsername(), "RECRUITER",
                            recruiterEntity.getId(), recruiterEntity.getEmail());
                } else {
                    return new Response<>(401, "Invalid username/email or password", null);
                }
            }
        } else {
            return new Response<>(400, "Invalid username/email or password", null);
        }
    }

    // Generate login tokens and refresh token
    private Response<?> generateTokensAndReturnResponse(String username, String role, Long id, String email) {
        String token = jwtUtil.generateToken(username, role);
        String refreshToken = refreshTokenService.createRefreshToken(username).getData();

        JwtResponse jwtResponse = JwtResponse.builder()
                .jwtToken(token)
                .refreshToken(refreshToken)
                .username(username)
                .role(role)
                .id(id)
                .email(email)
                .build();

        return new Response<>(200, role + " login successful", jwtResponse);
    }

    // Logout functionality
    public Response<?> logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return new Response<>(400, "Refresh token is required", null);
        }

        Response<String> response = refreshTokenService.deleteRefreshToken(refreshToken);

        if (response.getStatusCode() == 200) {
            return new Response<>(200, "Logout successful", null);
        } else {
            return new Response<>(400, "Invalid refresh token", null);
        }
    }

    // Refresh Access Token
    public Response<?> refreshAccessToken(String refreshToken) {
        Response<Boolean> validationResponse = refreshTokenService.validateRefreshToken(refreshToken);
        if (validationResponse.getData() == null || !validationResponse.getData()) {
            return new Response<>(401, "Invalid or expired Refresh Token", null);
        }

        Response<String> usernameResponse = refreshTokenService.getUsernameFromToken(refreshToken);
        String username = usernameResponse.getData();

        if (username == null) {
            return new Response<>(404, "Refresh Token not associated with any user", null);
        }

        String newJwtToken = jwtUtil.generateToken(username, "CANDIDATE");

        JwtResponse jwtResponse = JwtResponse.builder()
                .jwtToken(newJwtToken)
                .refreshToken(refreshToken)
                .username(username)
                .build();

        return new Response<>(200, "Token refreshed successfully", jwtResponse);
    }
}