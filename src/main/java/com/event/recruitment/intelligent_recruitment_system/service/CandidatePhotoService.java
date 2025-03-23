package com.event.recruitment.intelligent_recruitment_system.service;

import com.event.recruitment.intelligent_recruitment_system.dto.CandidateComcardDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.CandidatePhotoUploadRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.CandidateWorkingPhotoDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.Response;
import com.event.recruitment.intelligent_recruitment_system.model.CandidateSelfphotoComcard;
import com.event.recruitment.intelligent_recruitment_system.model.CandidateWorkingPhoto;
import com.event.recruitment.intelligent_recruitment_system.model.Candidates;
import com.event.recruitment.intelligent_recruitment_system.repository.CandidateComcardRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.CandidateWorkingPhotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CandidatePhotoService {

    @Autowired
    private CandidateWorkingPhotoRepository photoRepository;

    @Autowired
    private CandidateComcardRepository comcardRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Value("${file.upload-dir:uploads/photos}")
    private String photoUploadDir;

    @Value("${file.comcard-upload-dir:uploads/comcards}")
    private String comcardUploadDir;

    /**
     * Upload a working photo for the logged-in candidate
     */
    public Response<CandidateWorkingPhotoDTO> uploadWorkingPhoto(MultipartFile file, CandidatePhotoUploadRequest request) {
        try {
            // Get current logged-in user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Find candidate by username
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);
            if (candidateOpt.isEmpty()) {
                return new Response<>(404, "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Validate file
            if (file.isEmpty()) {
                return new Response<>(400, "Please upload a photo", null);
            }

            // Validate file type (accept only images)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return new Response<>(400, "Only image files are allowed", null);
            }

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(photoUploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate a unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String newFilename = UUID.randomUUID() + fileExtension;

            // Save the file
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            // Save to database
            String photoUrl = "/uploads/photos/" + newFilename;
            CandidateWorkingPhoto photo = new CandidateWorkingPhoto(
                    candidate.getId(),
                    photoUrl,
                    request.getDescription()
            );

            CandidateWorkingPhoto savedPhoto = photoRepository.save(photo);
            CandidateWorkingPhotoDTO photoDTO = new CandidateWorkingPhotoDTO(savedPhoto);

            return new Response<>(201, "Photo uploaded successfully", photoDTO);

        } catch (IOException e) {
            return new Response<>(500, "Failed to upload photo: " + e.getMessage(), null);
        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Get all working photos for the logged-in candidate
     */
    public Response<List<CandidateWorkingPhotoDTO>> getWorkingPhotos() {
        try {
            // Get current logged-in user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Find candidate by username
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);
            if (candidateOpt.isEmpty()) {
                return new Response<>(404, "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Get photos
            List<CandidateWorkingPhoto> photos = photoRepository.findByCandidateIdOrderByUploadedAtDesc(candidate.getId());
            List<CandidateWorkingPhotoDTO> photoDTOs = photos.stream()
                    .map(CandidateWorkingPhotoDTO::new)
                    .collect(Collectors.toList());

            return new Response<>(200, "Photos retrieved successfully", photoDTOs);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Delete a working photo
     */
    @Transactional
    public Response<Void> deleteWorkingPhoto(Long photoId) {
        try {
            // Get current logged-in user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Find candidate by username
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);
            if (candidateOpt.isEmpty()) {
                return new Response<>(404, "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Find photo
            Optional<CandidateWorkingPhoto> photoOpt = photoRepository.findById(photoId);
            if (photoOpt.isEmpty()) {
                return new Response<>(404, "Photo not found", null);
            }

            CandidateWorkingPhoto photo = photoOpt.get();

            // Check if photo belongs to the candidate
            if (!photo.getCandidateId().equals(candidate.getId())) {
                return new Response<>(403, "You don't have permission to delete this photo", null);
            }

            // Delete photo file
            String filename = photo.getPhotoUrl().substring(photo.getPhotoUrl().lastIndexOf("/") + 1);
            Path filePath = Paths.get(photoUploadDir).resolve(filename);
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log the error but continue with database deletion
                System.err.println("Failed to delete file: " + e.getMessage());
            }

            // Delete from database
            photoRepository.delete(photo);

            return new Response<>(200, "Photo deleted successfully", null);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Upload or update a comcard for the logged-in candidate
     */
    @Transactional
    public Response<CandidateComcardDTO> uploadOrUpdateComcard(MultipartFile file) {
        try {
            // Get current logged-in user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Find candidate by username
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);
            if (candidateOpt.isEmpty()) {
                return new Response<>(404, "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Validate file
            if (file.isEmpty()) {
                return new Response<>(400, "Please upload a comcard", null);
            }

            // Validate file type (accept only images)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return new Response<>(400, "Only image files are allowed", null);
            }

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(comcardUploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate a unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String newFilename = UUID.randomUUID() + fileExtension;

            // Check if candidate already has a comcard
            Optional<CandidateSelfphotoComcard> existingComcardOpt = comcardRepository.findByCandidateId(candidate.getId());

            // If exists, delete the old file
            if (existingComcardOpt.isPresent()) {
                CandidateSelfphotoComcard existingComcard = existingComcardOpt.get();
                String oldFilename = existingComcard.getComcardUrl().substring(existingComcard.getComcardUrl().lastIndexOf("/") + 1);
                Path oldFilePath = Paths.get(comcardUploadDir).resolve(oldFilename);
                try {
                    Files.deleteIfExists(oldFilePath);
                } catch (IOException e) {
                    // Log the error but continue with update
                    System.err.println("Failed to delete old comcard file: " + e.getMessage());
                }

                // Delete from database
                comcardRepository.delete(existingComcard);
            }

            // Save the new file
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            // Save to database
            String comcardUrl = "/uploads/comcards/" + newFilename;
            CandidateSelfphotoComcard comcard = new CandidateSelfphotoComcard(
                    candidate.getId(),
                    comcardUrl
            );

            CandidateSelfphotoComcard savedComcard = comcardRepository.save(comcard);
            CandidateComcardDTO comcardDTO = new CandidateComcardDTO(savedComcard);

            String message = existingComcardOpt.isPresent() ? "Comcard updated successfully" : "Comcard uploaded successfully";
            return new Response<>(200, message, comcardDTO);

        } catch (IOException e) {
            return new Response<>(500, "Failed to upload comcard: " + e.getMessage(), null);
        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Get the comcard for the logged-in candidate
     */
    public Response<CandidateComcardDTO> getComcard() {
        try {
            // Get current logged-in user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Find candidate by username
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);
            if (candidateOpt.isEmpty()) {
                return new Response<>(404, "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Get comcard
            Optional<CandidateSelfphotoComcard> comcardOpt = comcardRepository.findByCandidateId(candidate.getId());
            if (comcardOpt.isEmpty()) {
                return new Response<>(404, "Comcard not found", null);
            }

            CandidateComcardDTO comcardDTO = new CandidateComcardDTO(comcardOpt.get());

            return new Response<>(200, "Comcard retrieved successfully", comcardDTO);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Delete the comcard for the logged-in candidate
     */
    @Transactional
    public Response<Void> deleteComcard() {
        try {
            // Get current logged-in user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Find candidate by username
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);
            if (candidateOpt.isEmpty()) {
                return new Response<>(404, "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Find comcard
            Optional<CandidateSelfphotoComcard> comcardOpt = comcardRepository.findByCandidateId(candidate.getId());
            if (comcardOpt.isEmpty()) {
                return new Response<>(404, "Comcard not found", null);
            }

            CandidateSelfphotoComcard comcard = comcardOpt.get();

            // Delete comcard file
            String filename = comcard.getComcardUrl().substring(comcard.getComcardUrl().lastIndexOf("/") + 1);
            Path filePath = Paths.get(comcardUploadDir).resolve(filename);
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log the error but continue with database deletion
                System.err.println("Failed to delete comcard file: " + e.getMessage());
            }

            // Delete from database
            comcardRepository.delete(comcard);

            return new Response<>(200, "Comcard deleted successfully", null);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }
}