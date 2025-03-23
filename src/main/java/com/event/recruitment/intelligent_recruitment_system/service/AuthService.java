package com.event.recruitment.intelligent_recruitment_system.service;

import com.event.recruitment.intelligent_recruitment_system.dto.LoginRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.Response;
import com.event.recruitment.intelligent_recruitment_system.model.*;
import com.event.recruitment.intelligent_recruitment_system.repository.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.RecruiterRepository;
import com.event.recruitment.intelligent_recruitment_system.security.JwtUtil;
import com.event.recruitment.intelligent_recruitment_system.security.RefreshTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final CandidateRepository candidateRepository;
    private final RecruiterRepository recruiterRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public AuthService(CandidateRepository candidateRepository, RecruiterRepository recruiterRepository, JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
        this.candidateRepository = candidateRepository;
        this.recruiterRepository = recruiterRepository;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    // Candidate 登录 - 使用用户名和密码
    public Response<Map<String, String>> candidateLogin(LoginRequest loginRequest) {
        return loginOrRegister(loginRequest, "CANDIDATE");
    }

    // Recruiter 登录 - 使用用户名和密码
    public Response<Map<String, String>> recruiterLogin(LoginRequest loginRequest) {
        return loginOrRegister(loginRequest, "RECRUITER");
    }

    // 通用登录/注册方法
    private Response<Map<String, String>> loginOrRegister(LoginRequest loginRequest, String role) {
        Optional<?> userEntityOptional = (role.equals("CANDIDATE"))
                ? candidateRepository.findByUsername(loginRequest.getUsername())
                : recruiterRepository.findByUsername(loginRequest.getUsername());

        if (userEntityOptional.isPresent()) {
            if (role.equals("CANDIDATE")) {
                Candidates candidateEntity = (Candidates) userEntityOptional.get();
                return generateTokensAndReturnResponse(candidateEntity.getUsername(), "CANDIDATE");
            } else {
                Recruiters recruiterEntity = (Recruiters) userEntityOptional.get();
                return generateTokensAndReturnResponse(recruiterEntity.getUsername(), "RECRUITER");
            }
        } else {
            return Response.<Map<String, String>>builder()
                    .statusCode(400)
                    .message("Invalid username or password")
                    .data(null)
                    .build();
        }
    }

    // OAuth 登录 - 处理 Google/Facebook 登录
    public Response<Map<String, String>> oauthLogin(String oauthId, String email, String authProvider, String role) {
        return handleLoginOrRegister(email, oauthId, authProvider, role);
    }

    // 通用的登录或注册方法
    private Response<Map<String, String>> handleLoginOrRegister(String email, String oauthId, String authProvider, String role) {
        Optional<?> userEntityOptional = (role.equals("CANDIDATE"))
                ? candidateRepository.findByEmail(email)
                : recruiterRepository.findByEmail(email);

        if (userEntityOptional.isPresent()) {
            if (role.equals("CANDIDATE")) {
                Candidates candidateEntity = (Candidates) userEntityOptional.get();
                return generateTokensAndReturnResponse(candidateEntity.getEmail(), "CANDIDATE");
            } else {
                Recruiters recruiterEntity = (Recruiters) userEntityOptional.get();
                return generateTokensAndReturnResponse(recruiterEntity.getEmail(), "RECRUITER");
            }
        } else {
            if (role.equals("CANDIDATE")) {
                Candidates candidateEntity = new Candidates();
                candidateEntity.setEmail(email);
                candidateEntity.setOauthId(oauthId);
                candidateEntity.setAuthProvider(AuthProvider.valueOf(authProvider)); // GOOGLE 或 FACEBOOK
                candidateEntity.setUsername(email.split("@")[0]); // 默认 username 为 email 的前缀
                // 填充默认值
                candidateEntity.setPhoneNumber("");
                candidateEntity.setGender(Gender.OTHER);  // 默认设置为 OTHER
                candidateEntity.setRace(Race.OTHER);  // 默认设置为 "Other"
                candidateEntity.setDateOfBirth(null);  // 可以为空，后续补充
                candidateRepository.save(candidateEntity);
                return generateTokensAndReturnResponse(candidateEntity.getEmail(), "CANDIDATE");
            } else {
                Recruiters recruiterEntity = new Recruiters();
                recruiterEntity.setEmail(email);
                recruiterEntity.setOauthId(oauthId);
                recruiterEntity.setAuthProvider(AuthProvider.valueOf(authProvider)); // GOOGLE 或 FACEBOOK
                recruiterEntity.setUsername(email.split("@")[0]);  // 默认 username 为 email 的前缀
                // 填充默认值
                recruiterEntity.setPhoneNumber("");
                recruiterEntity.setRecruiterRepName("");  // 默认空，后续补充
                recruiterEntity.setRecruiterType(RecruiterType.INDIVIDUAL);  // 默认是 Individual
                recruiterRepository.save(recruiterEntity);
                return generateTokensAndReturnResponse(recruiterEntity.getEmail(), "RECRUITER");
            }
        }
    }

    // 生成登录后的 Token 和 Refresh Token
    private Response<Map<String, String>> generateTokensAndReturnResponse(String email, String role) {
        String token = jwtUtil.generateToken(email, role);
        String refreshToken = refreshTokenService.createRefreshToken(email).getData();

        Map<String, String> tokens = new HashMap<>();
        tokens.put("jwtToken", token);
        tokens.put("refreshToken", refreshToken);

        return Response.<Map<String, String>>builder()
                .statusCode(200)
                .message(role + " login successful")
                .data(tokens)
                .build();
    }

    // 登出功能
    public Response<String> logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return Response.<String>builder()
                    .statusCode(400)
                    .message("Refresh token is required")
                    .data(null)
                    .build();
        }

        Response<String> response = refreshTokenService.deleteRefreshToken(refreshToken);

        if (response.getStatusCode() == 200) {
            return Response.<String>builder()
                    .statusCode(200)
                    .message("Logout successful")
                    .data(null)
                    .build();
        } else {
            return Response.<String>builder()
                    .statusCode(400)
                    .message("Invalid refresh token")
                    .data(null)
                    .build();
        }
    }

    // 刷新 Access Token
    public Response<String> refreshAccessToken(String refreshToken) {
        Response<Boolean> validationResponse = refreshTokenService.validateRefreshToken(refreshToken);
        if (validationResponse.getData() == null || !validationResponse.getData()) {
            return Response.<String>builder()
                    .statusCode(401)
                    .message("Invalid or expired Refresh Token")
                    .data(null)
                    .build();
        }

        Response<String> usernameResponse = refreshTokenService.getUsernameFromToken(refreshToken);
        String username = usernameResponse.getData();

        if (username == null) {
            return Response.<String>builder()
                    .statusCode(404)
                    .message("Refresh Token not associated with any user")
                    .data(null)
                    .build();
        }

        String newJwtToken = jwtUtil.generateToken(username, "CANDIDATE");

        return Response.<String>builder()
                .statusCode(200)
                .message("Token refreshed successfully")
                .data(newJwtToken)
                .build();
    }
}
