package com.event.recruitment.intelligent_recruitment_system.controller.common;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.common.ChangePasswordRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.common.UpdateProfileRequest;
import com.event.recruitment.intelligent_recruitment_system.service.common.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<Response<?>> getProfile() {
        try {
            Response<?> response = profileService.getProfile();
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error fetching profile", null));
        }
    }

    @PutMapping
    public ResponseEntity<Response<?>> updateProfile(@RequestBody UpdateProfileRequest updateRequest) {
        try {
            Response<?> response = profileService.updateProfile(updateRequest);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error updating profile", null));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<Response<?>> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            Response<?> response = profileService.changePassword(request);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error changing password: " + e.getMessage(), null));
        }
    }

    // Add to ProfileController.java
    @PutMapping("/update-email")
    public ResponseEntity<Response<?>> updateCandidateEmail(@RequestParam String newEmail) {
        try {
            Response<?> response = profileService.updateCandidateEmail(newEmail);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error updating email: " + e.getMessage(), null));
        }
    }
}
