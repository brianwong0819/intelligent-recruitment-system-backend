package com.event.recruitment.intelligent_recruitment_system.controller.candidate;

import com.event.recruitment.intelligent_recruitment_system.dto.request.candidate.CandidatePhotoUploadRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.service.candidate.CandidateFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/candidate/file")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_CANDIDATE')")
public class CandidateFileController {

    private final CandidateFileService photoService;

    /**
     * Upload a working photo
     */
    @PostMapping(value = "/working", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response<?>> uploadWorkingPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        try {
            CandidatePhotoUploadRequest request = new CandidatePhotoUploadRequest(description);
            Response<?> response = photoService.uploadWorkingPhoto(file, request);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error uploading photo: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Get all working photos
     */
    @GetMapping("/working")
    public ResponseEntity<Response<?>> getWorkingPhotos() {
        try {
            Response<?> response = photoService.getWorkingPhotos();
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving photos: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Delete a working photo
     */
    @DeleteMapping("/working/{photoId}")
    public ResponseEntity<Response<?>> deleteWorkingPhoto(@PathVariable Long photoId) {
        try {
            Response<?> response = photoService.deleteWorkingPhoto(photoId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error deleting photo: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Upload a new comcard (max 3)
     */
    @PostMapping(value = "/comcard", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response<?>> uploadComcard(
            @RequestParam("file") MultipartFile file) {
        try {
            Response<?> response = photoService.uploadComcard(file);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error uploading comcard: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Get all comcards for the candidate
     */
    @GetMapping("/comcard")
    public ResponseEntity<Response<?>> getComcards() {
        try {
            Response<?> response = photoService.getComcards();
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving comcards: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Delete a specific comcard
     */
    @DeleteMapping("/comcard/{comcardId}")
    public ResponseEntity<Response<?>> deleteComcard(@PathVariable Long comcardId) {
        try {
            Response<?> response = photoService.deleteComcard(comcardId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error deleting comcard: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Upload or update a profile picture
     */
    @PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response<?>> uploadOrUpdateProfilePicture(
            @RequestParam("file") MultipartFile file) {
        try {
            Response<?> response = photoService.uploadOrUpdateProfilePicture(file);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error uploading profile picture: " + e.getMessage(), null)
            );
        }
    }

    @PostMapping("/resume")
    public ResponseEntity<Response<?>> uploadOrUpdateResume(@RequestParam("file") MultipartFile file) {
        try {
            Response<?> response = photoService.uploadOrUpdateResume(file);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error uploading resume", null));
        }
    }

    @DeleteMapping("/resume")
    public ResponseEntity<Response<?>> deleteResume() {
        try {
            Response<?> response = photoService.deleteResume();
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error deleting resume", null));
        }
    }

    @GetMapping("/resume")
    public ResponseEntity<?> downloadResume() {
        try {
            return photoService.downloadResume();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error downloading resume", null));
        }
    }
}