// Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/controller/auth/AdminAuthController.java

package com.event.recruitment.intelligent_recruitment_system.controller.auth;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.auth.AdminLoginRequest;
import com.event.recruitment.intelligent_recruitment_system.service.auth.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/admin/login")
    public ResponseEntity<Response<?>> adminLogin(@Valid @RequestBody AdminLoginRequest loginRequest) {
        Response<?> response = adminAuthService.login(loginRequest);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}