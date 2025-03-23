package com.event.recruitment.intelligent_recruitment_system.security;

import com.event.recruitment.intelligent_recruitment_system.dto.Response;
import com.event.recruitment.intelligent_recruitment_system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    public OAuth2LoginSuccessHandler(AuthService authService) {
        this.authService = authService;
    }

    // 实现 AuthenticationSuccessHandler 接口中的 onAuthenticationSuccess 方法
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        // 获取 OAuth2User 对象
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        String oauthId = oauthUser.getAttribute("sub"); // Google/Facebook 唯一标识符

        // 如果没有获取到必要的属性，重定向到错误页面
        if (email == null || oauthId == null) {
            response.sendRedirect("/error?message=Missing%20OAuth2%20attributes");
            return;
        }

        // 获取前端传递的角色信息
        String role = request.getParameter("role"); // 从请求参数中获取角色信息

        if (role == null || role.isEmpty()) {
            response.sendRedirect("/error?message=Role%20is%20missing");
            return;
        }

        // 调用 AuthService 进行登录或注册
        Response<Map<String, String>> loginResponse = authService.oauthLogin(oauthId, email, "GOOGLE", role);
        Map<String, String> tokens = loginResponse.getData();

        // 重定向到主页，并将 JWT 和 Refresh Token 作为查询参数传递
        response.sendRedirect("/home?jwtToken=" + tokens.get("jwtToken") + "&refreshToken=" + tokens.get("refreshToken"));
    }
}
