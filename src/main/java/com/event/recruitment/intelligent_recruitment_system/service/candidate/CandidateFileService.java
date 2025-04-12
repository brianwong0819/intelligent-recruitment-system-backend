package com.event.recruitment.intelligent_recruitment_system.service.candidate;

import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateComcardDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.request.candidate.CandidatePhotoUploadRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateWorkingPhotoDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateSelfphotoComcard;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateWorkingPhoto;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateComcardRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateWorkingPhotoRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Service
@RequiredArgsConstructor
public class CandidateFileService {

    private final CandidateWorkingPhotoRepository photoRepository;
    private final CandidateComcardRepository comcardRepository;
    private final CandidateRepository candidateRepository;
    private final SecurityUtil securityUtil;

    @Value("${file.upload-dir:C:/Users/Acer/OneDrive/Desktop/fyp/Frontend Code/event-recruitment-frontend/src/assets/working-photos}")
    private String photoUploadDir;

    @Value("${file.comcard-upload-dir:C:/Users/Acer/OneDrive/Desktop/fyp/Frontend Code/event-recruitment-frontend/src/assets/comcards}")
    private String comcardUploadDir;

    @Value("${file.profile-pic-upload-dir:C:/Users/Acer/OneDrive/Desktop/fyp/Frontend Code/event-recruitment-frontend/src/assets/profile-pictures}")
    private String profilePicUploadDir;

    @Value("${file.resume-upload-dir:C:/Users/Acer/OneDrive/Desktop/fyp/Frontend Code/event-recruitment-frontend/src/assets/resumes}")
    private String resumeUploadDir;

    /**
     * Upload a working photo for the logged-in candidate (maximum 3 allowed)
     */
    public Response<?> uploadWorkingPhoto(MultipartFile file, CandidatePhotoUploadRequest request) {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

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

            // Get existing working photos to check count
            List<CandidateWorkingPhoto> existingPhotos = photoRepository.findByCandidateIdOrderByUploadedAtDesc(candidate.getId());

            // Check if candidate already has 3 working photos
            if (existingPhotos.size() >= 3) {
                return new Response<>(400, "Maximum of 3 working photos allowed. Please delete an existing photo before uploading a new one.", null);
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
            String photoUrl = "/assets/working-photos/" + newFilename;
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
    public Response<?> getWorkingPhotos() {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

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
    public Response<?> deleteWorkingPhoto(Long photoId) {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

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
     * Upload a comcard for the logged-in candidate (maximum 3 allowed)
     */
    @Transactional
    public Response<?> uploadComcard(MultipartFile file) {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

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

            // Get existing comcards to check count
            List<CandidateSelfphotoComcard> existingComcards = comcardRepository.findAllByCandidateId(candidate.getId());

            // Check if candidate already has 3 comcards
            if (existingComcards.size() >= 3) {
                return new Response<>(400, "Maximum of 3 comcards allowed. Please delete an existing comcard before uploading a new one.", null);
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

            // Save the new file
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            // Save to database
            String comcardUrl = "/assets/comcards/" + newFilename;
            CandidateSelfphotoComcard comcard = new CandidateSelfphotoComcard(
                    candidate.getId(),
                    comcardUrl
            );

            CandidateSelfphotoComcard savedComcard = comcardRepository.save(comcard);
            CandidateComcardDTO comcardDTO = new CandidateComcardDTO(savedComcard);

            return new Response<>(201, "Comcard uploaded successfully", comcardDTO);

        } catch (IOException e) {
            return new Response<>(500, "Failed to upload comcard: " + e.getMessage(), null);
        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Get all comcards for the logged-in candidate
     */
    public Response<?> getComcards() {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

            // Find candidate by username
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);
            if (candidateOpt.isEmpty()) {
                return new Response<>(404, "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Get comcards
            List<CandidateSelfphotoComcard> comcards = comcardRepository.findAllByCandidateId(candidate.getId());

            if (comcards.isEmpty()) {
                return new Response<>(200, "No comcards found", new ArrayList<>());
            }

            List<CandidateComcardDTO> comcardDTOs = comcards.stream()
                    .map(CandidateComcardDTO::new)
                    .collect(Collectors.toList());

            return new Response<>(200, "Comcards retrieved successfully", comcardDTOs);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Delete a specific comcard for the logged-in candidate
     */
    @Transactional
    public Response<?> deleteComcard(Long comcardId) {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

            // Find candidate by username
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);
            if (candidateOpt.isEmpty()) {
                return new Response<>(404, "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Find comcard
            Optional<CandidateSelfphotoComcard> comcardOpt = comcardRepository.findById(comcardId);
            if (comcardOpt.isEmpty()) {
                return new Response<>(404, "Comcard not found", null);
            }

            CandidateSelfphotoComcard comcard = comcardOpt.get();

            // Check if comcard belongs to the candidate
            if (!comcard.getCandidateId().equals(candidate.getId())) {
                return new Response<>(403, "You don't have permission to delete this comcard", null);
            }

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

    /**
     * Upload or update profile picture for the logged-in candidate
     */
    @Transactional
    public Response<?> uploadOrUpdateProfilePicture(MultipartFile file) {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

            // Find candidate by username
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);
            if (candidateOpt.isEmpty()) {
                return new Response<>(404, "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Validate file
            if (file.isEmpty()) {
                return new Response<>(400, "Please upload a profile picture", null);
            }

            // Validate file type (accept only images)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return new Response<>(400, "Only image files are allowed", null);
            }

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(profilePicUploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate a unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String newFilename = UUID.randomUUID() + fileExtension;

            // If user already has a profile picture, delete the old file
            if (candidate.getProfilePictureUrl() != null && !candidate.getProfilePictureUrl().isEmpty()
                    && !candidate.getProfilePictureUrl().startsWith("http")) {
                String oldFilename = candidate.getProfilePictureUrl().substring(candidate.getProfilePictureUrl().lastIndexOf("/") + 1);
                Path oldFilePath = Paths.get(profilePicUploadDir).resolve(oldFilename);
                try {
                    Files.deleteIfExists(oldFilePath);
                } catch (IOException e) {
                    // Log the error but continue with update
                    System.err.println("Failed to delete old profile picture file: " + e.getMessage());
                }
            }

            // Save the new file
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            // Update candidate's profile picture URL
            String profilePictureUrl = "/assets/profile-pictures/" + newFilename;
            candidate.setProfilePictureUrl(profilePictureUrl);
            candidateRepository.save(candidate);

            return new Response<>(200, "Profile picture updated successfully", candidate);

        } catch (IOException e) {
            return new Response<>(500, "Failed to upload profile picture: " + e.getMessage(), null);
        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Upload or update resume for the logged-in candidate
     */
    @Transactional
    public Response<?> uploadOrUpdateResume(MultipartFile file) {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

            // Find candidate by username
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);
            if (candidateOpt.isEmpty()) {
                return new Response<>(404, "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Validate file
            if (file.isEmpty()) {
                return new Response<>(400, "Please upload a resume", null);
            }

            // Validate file type (accept only PDF or DOC/DOCX)
            String contentType = file.getContentType();
            if (contentType == null ||
                    !(contentType.equals("application/pdf") ||
                            contentType.equals("application/msword") ||
                            contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
                return new Response<>(400, "Only PDF or DOC/DOCX files are allowed", null);
            }

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(resumeUploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate a unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".pdf";
            String newFilename = UUID.randomUUID() + fileExtension;

            // If user already has a resume, delete the old file
            if (candidate.getResumeUrl() != null && !candidate.getResumeUrl().isEmpty()
                    && !candidate.getResumeUrl().startsWith("http")) {
                String oldFilename = candidate.getResumeUrl().substring(candidate.getResumeUrl().lastIndexOf("/") + 1);
                Path oldFilePath = Paths.get(resumeUploadDir).resolve(oldFilename);
                try {
                    Files.deleteIfExists(oldFilePath);
                } catch (IOException e) {
                    // Log the error but continue with update
                    System.err.println("Failed to delete old resume file: " + e.getMessage());
                }
            }

            // Save the new file
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            // Update candidate's resume URL
            String resumeUrl = "/assets/resumes/" + newFilename;
            candidate.setResumeUrl(resumeUrl);
            candidateRepository.save(candidate);

            return new Response<>(200, "Resume updated successfully", candidate);

        } catch (IOException e) {
            return new Response<>(500, "Failed to upload resume: " + e.getMessage(), null);
        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Delete the resume for the logged-in candidate
     */
    @Transactional
    public Response<?> deleteResume() {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

            // Find candidate by username
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);
            if (candidateOpt.isEmpty()) {
                return new Response<>(404, "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Check if candidate has a resume
            if (candidate.getResumeUrl() == null || candidate.getResumeUrl().isEmpty()) {
                return new Response<>(404, "Resume not found", null);
            }

            // Delete resume file
            String filename = candidate.getResumeUrl().substring(candidate.getResumeUrl().lastIndexOf("/") + 1);
            Path filePath = Paths.get(resumeUploadDir).resolve(filename);
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log the error but continue with database update
                System.err.println("Failed to delete resume file: " + e.getMessage());
            }

            // Update candidate's resume URL
            candidate.setResumeUrl(null);
            candidateRepository.save(candidate);

            return new Response<>(200, "Resume deleted successfully", null);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Download the resume for the logged-in candidate
     */
    public ResponseEntity<?> downloadResume() {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

            // Find candidate by username
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);
            if (candidateOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Candidates candidate = candidateOpt.get();

            // Check if candidate has a resume
            if (candidate.getResumeUrl() == null || candidate.getResumeUrl().isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Extract filename from URL
            String resumeUrl = candidate.getResumeUrl();
            String filename = resumeUrl.substring(resumeUrl.lastIndexOf("/") + 1);

            // Create path to file
            Path filePath = Paths.get(resumeUploadDir).resolve(filename);

            // Check if file exists
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // Load file as Resource
            Resource resource = new UrlResource(filePath.toUri());

            // Try to determine file's content type
            String contentType = null;
            try {
                contentType = Files.probeContentType(filePath);
            } catch (IOException ex) {
                System.err.println("Could not determine file type: " + ex.getMessage());
            }

            // Fallback to generic octet-stream
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Determine original filename for download
            String originalFilename = "resume";
            String extension = filename.substring(filename.lastIndexOf("."));
            originalFilename += extension;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFilename + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new Response<>(500, "An error occurred: " + e.getMessage(), null)
            );
        }
    }
}