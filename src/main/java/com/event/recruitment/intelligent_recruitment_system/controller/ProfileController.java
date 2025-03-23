package com.event.recruitment.intelligent_recruitment_system.controller;
import com.event.recruitment.intelligent_recruitment_system.dto.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.UpdateProfileRequest;
import com.event.recruitment.intelligent_recruitment_system.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // 获取当前用户的个人资料 (Candidate 或 Recruiter)
    @GetMapping
    public ResponseEntity<Response<?>> getProfile() {
        try {
            Response<?> response = profileService.getProfile();
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error fetching profile", null));
        }
    }

    // 更新个人资料
    @PutMapping
    public ResponseEntity<Response<?>> updateProfile(@RequestBody UpdateProfileRequest updateRequest) {
        try {
            Response<?> response = profileService.updateProfile(updateRequest);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error updating profile", null));
        }
    }
}
