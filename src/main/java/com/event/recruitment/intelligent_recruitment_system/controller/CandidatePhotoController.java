package com.event.recruitment.intelligent_recruitment_system.controller;

import com.event.recruitment.intelligent_recruitment_system.dto.CandidateComcardDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.CandidatePhotoUploadRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.CandidateWorkingPhotoDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.Response;
import com.event.recruitment.intelligent_recruitment_system.service.CandidatePhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/candidate/photos")
public class CandidatePhotoController {

    @Autowired
    private CandidatePhotoService photoService;

    /**
     * Upload a working photo
     */
    @PostMapping(value = "/working", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response<CandidateWorkingPhotoDTO>> uploadWorkingPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        try {
            CandidatePhotoUploadRequest request = new CandidatePhotoUploadRequest(description);
            Response<CandidateWorkingPhotoDTO> response = photoService.uploadWorkingPhoto(file, request);
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
    public ResponseEntity<Response<List<CandidateWorkingPhotoDTO>>> getWorkingPhotos() {
        try {
            Response<List<CandidateWorkingPhotoDTO>> response = photoService.getWorkingPhotos();
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
    public ResponseEntity<Response<Void>> deleteWorkingPhoto(@PathVariable Long photoId) {
        try {
            Response<Void> response = photoService.deleteWorkingPhoto(photoId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error deleting photo: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Upload or update a comcard
     */
    @PostMapping(value = "/comcard", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response<CandidateComcardDTO>> uploadOrUpdateComcard(
            @RequestParam("file") MultipartFile file) {
        try {
            Response<CandidateComcardDTO> response = photoService.uploadOrUpdateComcard(file);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error uploading comcard: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Get the comcard
     */
    @GetMapping("/comcard")
    public ResponseEntity<Response<CandidateComcardDTO>> getComcard() {
        try {
            Response<CandidateComcardDTO> response = photoService.getComcard();
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving comcard: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Delete the comcard
     */
    @DeleteMapping("/comcard")
    public ResponseEntity<Response<Void>> deleteComcard() {
        try {
            Response<Void> response = photoService.deleteComcard();
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error deleting comcard: " + e.getMessage(), null)
            );
        }
    }
}