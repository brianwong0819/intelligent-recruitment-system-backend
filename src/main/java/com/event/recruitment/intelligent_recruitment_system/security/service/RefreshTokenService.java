package com.event.recruitment.intelligent_recruitment_system.security.service;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.model.entity.security.RefreshToken;
import com.event.recruitment.intelligent_recruitment_system.repository.security.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${jwt.refresh.expiration}")
    private long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * 生成新的 Refresh Token，每次登录都会创建新的 Token，删除旧的 Token
     */
    @Transactional
    public Response<String> createRefreshToken(String username) {
        // 先删除旧的 Token，确保唯一性
        refreshTokenRepository.deleteByUsername(username);

        // 生成新的 Refresh Token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsername(username);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));

        refreshTokenRepository.save(refreshToken);

        return Response.<String>builder()
                .statusCode(200)
                .message("Refresh token created successfully")
                .data(refreshToken.getToken())
                .build();
    }

    /**
     * 验证 Refresh Token 是否有效
     */
    public Response<Boolean> validateRefreshToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        boolean isValid = refreshToken.isPresent() && refreshToken.get().getExpiryDate().isAfter(Instant.now());

        return Response.<Boolean>builder()
                .statusCode(isValid ? 200 : 401)
                .message(isValid ? "Valid Refresh Token" : "Invalid or Expired Refresh Token")
                .data(isValid)
                .build();
    }

    /**
     * 根据 Refresh Token 获取用户名
     */
    public Response<String> getUsernameFromToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        return refreshToken.map(rt -> Response.<String>builder()
                        .statusCode(200)
                        .message("Username retrieved successfully")
                        .data(rt.getUsername())
                        .build())
                .orElse(Response.<String>builder()
                        .statusCode(404)
                        .message("Refresh token not found")
                        .data(null)
                        .build());
    }

    /**
     * 删除指定 Refresh Token
     */
    @Transactional
    public Response<String> deleteRefreshToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        if (refreshToken.isPresent()) {
            refreshTokenRepository.deleteByToken(token);
            return Response.<String>builder()
                    .statusCode(200)
                    .message("Refresh token deleted successfully")
                    .data(null)
                    .build();
        } else {
            return Response.<String>builder()
                    .statusCode(404)
                    .message("Refresh token not found")
                    .data(null)
                    .build();
        }
    }

    /**
     * 删除用户所有的 Refresh Token（用于 Logout）
     */
    @Transactional
    public Response<String> deleteAllRefreshTokensForUser(String username) {
        int deletedCount = refreshTokenRepository.deleteByUsername(username);
        return Response.<String>builder()
                .statusCode(deletedCount > 0 ? 200 : 404)
                .message(deletedCount > 0 ? "All refresh tokens deleted successfully" : "No refresh tokens found")
                .data(null)
                .build();
    }
}
