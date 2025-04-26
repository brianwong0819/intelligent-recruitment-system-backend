package com.event.recruitment.intelligent_recruitment_system.service.training;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.training.UpdateTrainingStatusRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.training.UploadTrainingMaterialRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.training.TrainingMaterialResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.training.TrainingMaterial;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Recruiters;
import com.event.recruitment.intelligent_recruitment_system.repository.training.TrainingMaterialRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.recruiter.RecruiterRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingMaterialService {

    private final TrainingMaterialRepository trainingMaterialRepository;
    private final RecruiterRepository recruiterRepository;
    private final SecurityUtil securityUtil;

    @Value("${file.training-materials-dir}")
    private String trainingMaterialsDir;

    /**
     * Upload a new training material for a job
     *
     * @param jobId ID of the job to upload training material for
     * @param file The PDF file to upload
     * @param request Additional details like description
     * @return Response containing the uploaded training material details
     */
    @Transactional
    public Response<?> uploadTrainingMaterial(Long jobId, MultipartFile file, UploadTrainingMaterialRequest request) {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

            // Find recruiter by username
            Optional<Recruiters> recruiterOpt = recruiterRepository.findByUsername(username);
            if (recruiterOpt.isEmpty()) {
                return new Response<>(404, "Recruiter not found", null);
            }

            Recruiters recruiter = recruiterOpt.get();

            // Validate file
            if (file.isEmpty()) {
                return new Response<>(400, "Please upload a training material", null);
            }

            // Validate file type (accept only PDF)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                return new Response<>(400, "Only PDF files are allowed for training materials", null);
            }

            // Validate file size (max 20MB)
            if (file.getSize() > 20 * 1024 * 1024) { // 20MB in bytes
                return new Response<>(400, "File size exceeds maximum limit of 20MB", null);
            }

            // Check if this job already has a training material
            if (trainingMaterialRepository.existsByJobIdAndIsActiveTrue(jobId)) {
                return new Response<>(400, "This job already has an active training material. Please delete it first before uploading a new one.", null);
            }

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(trainingMaterialsDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate a unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".pdf";
            String newFilename = UUID.randomUUID() + fileExtension;

            // Save the file
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            // Save to database
            String fileUrl = "/api/training/materials/" + newFilename;
            TrainingMaterial trainingMaterial = new TrainingMaterial(
                    jobId,
                    originalFilename != null ? originalFilename : "training-material.pdf",
                    fileUrl,
                    file.getSize(),
                    contentType,
                    request.getDescription(),
                    recruiter.getId()
            );

            // Training material is disabled by default (constructor sets isEnabled = false)
            TrainingMaterial savedMaterial = trainingMaterialRepository.save(trainingMaterial);
            TrainingMaterialResponseDTO responseDTO = new TrainingMaterialResponseDTO(savedMaterial);

            return new Response<>(201, "Training material uploaded successfully. Note: Training is currently disabled. Please enable it once you are ready to make it available to candidates.", responseDTO);

        } catch (IOException e) {
            return new Response<>(500, "Failed to upload training material: " + e.getMessage(), null);
        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Enable or disable a training material
     *
     * @param jobId ID of the job
     * @param materialId ID of the training material
     * @param request Contains the isEnabled flag
     * @return Response containing the updated training material details
     */
    @Transactional
    public Response<?> updateTrainingStatus(Long jobId, Long materialId, UpdateTrainingStatusRequest request) {
        try {
            String username = securityUtil.getCurrentUsername();

            Optional<Recruiters> recruiterOpt = recruiterRepository.findByUsername(username);
            if (recruiterOpt.isEmpty()) {
                return new Response<>(404, "Recruiter not found", null);
            }

            Optional<TrainingMaterial> materialOpt = trainingMaterialRepository.findByIdAndJobIdAndIsActiveTrue(materialId, jobId);
            if (materialOpt.isEmpty()) {
                return new Response<>(404, "Training material not found", null);
            }

            TrainingMaterial material = materialOpt.get();

            material.setIsEnabled(request.getIsEnabled());
            material.setUpdatedAt(LocalDateTime.now());

            TrainingMaterial updatedMaterial = trainingMaterialRepository.save(material);
            TrainingMaterialResponseDTO responseDTO = new TrainingMaterialResponseDTO(updatedMaterial);

            String message = request.getIsEnabled() ?
                    "Training material enabled successfully. It is now available to candidates." :
                    "Training material disabled successfully. It is now hidden from candidates.";

            return new Response<>(200, message, responseDTO);

        } catch (Exception e) {
            e.printStackTrace();
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Get training material for a specific job
     *
     * @param jobId ID of the job to get training material for
     * @return Response containing the training material details
     */
    public Response<?> getTrainingMaterialsByJob(Long jobId) {
        try {
            // Get training materials
            List<TrainingMaterial> materials = trainingMaterialRepository.findByJobIdAndIsActiveTrue(jobId);

            if (materials.isEmpty()) {
                return new Response<>(200, "No training materials found for this job", List.of());
            }

            List<TrainingMaterialResponseDTO> materialDTOs = materials.stream()
                    .map(TrainingMaterialResponseDTO::new)
                    .collect(Collectors.toList());

            return new Response<>(200, "Training materials retrieved successfully", materialDTOs);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Get training material for a specific job (for candidates)
     * Only returns enabled training materials
     *
     * @param jobId ID of the job to get training material for
     * @return Response containing the training material details
     */
    public Response<?> getEnabledTrainingMaterialsByJob(Long jobId) {
        try {
            // Get training materials that are enabled
            List<TrainingMaterial> materials = trainingMaterialRepository.findByJobIdAndIsActiveTrueAndIsEnabledTrue(jobId);

            if (materials.isEmpty()) {
                return new Response<>(200, "No training materials found for this job", List.of());
            }

            List<TrainingMaterialResponseDTO> materialDTOs = materials.stream()
                    .map(TrainingMaterialResponseDTO::new)
                    .collect(Collectors.toList());

            return new Response<>(200, "Training materials retrieved successfully", materialDTOs);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Download a specific training material
     *
     * @param materialId ID of the training material to download
     * @return ResponseEntity containing the file resource
     */
    public ResponseEntity<?> downloadTrainingMaterial(Long materialId) {
        try {
            // Find training material
            Optional<TrainingMaterial> materialOpt = trainingMaterialRepository.findById(materialId);
            if (materialOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            TrainingMaterial material = materialOpt.get();

            // Extract filename from URL
            String fileUrl = material.getFileUrl();
            String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            // Create path to file
            Path filePath = Paths.get(trainingMaterialsDir).resolve(filename);

            // Check if file exists
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // Load file as Resource
            Resource resource = new UrlResource(filePath.toUri());

            // Try to determine file's content type
            String contentType = material.getMimeType();
            if (contentType == null) {
                contentType = "application/pdf"; // Default to PDF
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + material.getFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new Response<>(500, "An error occurred: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Delete a training material
     *
     * @param jobId ID of the job the training material belongs to
     * @param materialId ID of the training material to delete
     * @return Response indicating success or failure
     */
    @Transactional
    public Response<?> deleteTrainingMaterial(Long jobId, Long materialId) {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

            // Find recruiter by username
            Optional<Recruiters> recruiterOpt = recruiterRepository.findByUsername(username);
            if (recruiterOpt.isEmpty()) {
                return new Response<>(404, "Recruiter not found", null);
            }

            // Find training material
            Optional<TrainingMaterial> materialOpt = trainingMaterialRepository.findByIdAndJobIdAndIsActiveTrue(materialId, jobId);
            if (materialOpt.isEmpty()) {
                return new Response<>(404, "Training material not found", null);
            }

            TrainingMaterial material = materialOpt.get();

            // Delete material file
            String filename = material.getFileUrl().substring(material.getFileUrl().lastIndexOf("/") + 1);
            Path filePath = Paths.get(trainingMaterialsDir).resolve(filename);
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log the error but continue with database update
                System.err.println("Failed to delete training material file: " + e.getMessage());
            }

            // Soft delete in database by setting isActive to false
            material.setIsActive(false);
            material.setUpdatedAt(LocalDateTime.now());
            trainingMaterialRepository.save(material);

            return new Response<>(200, "Training material deleted successfully", null);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Update a training material's description
     *
     * @param jobId ID of the job the training material belongs to
     * @param materialId ID of the training material to update
     * @param request Updated details
     * @return Response containing the updated training material details
     */
    @Transactional
    public Response<?> updateTrainingMaterial(Long jobId, Long materialId, UploadTrainingMaterialRequest request) {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

            // Find recruiter by username
            Optional<Recruiters> recruiterOpt = recruiterRepository.findByUsername(username);
            if (recruiterOpt.isEmpty()) {
                return new Response<>(404, "Recruiter not found", null);
            }

            // Find training material
            Optional<TrainingMaterial> materialOpt = trainingMaterialRepository.findByIdAndJobIdAndIsActiveTrue(materialId, jobId);
            if (materialOpt.isEmpty()) {
                return new Response<>(404, "Training material not found", null);
            }

            TrainingMaterial material = materialOpt.get();

            // Update description
            material.setDescription(request.getDescription());
            material.setUpdatedAt(LocalDateTime.now());

            TrainingMaterial updatedMaterial = trainingMaterialRepository.save(material);
            TrainingMaterialResponseDTO responseDTO = new TrainingMaterialResponseDTO(updatedMaterial);

            return new Response<>(200, "Training material updated successfully", responseDTO);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }
}