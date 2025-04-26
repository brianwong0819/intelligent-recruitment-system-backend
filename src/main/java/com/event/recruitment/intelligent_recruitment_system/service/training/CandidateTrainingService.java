package com.event.recruitment.intelligent_recruitment_system.service.training;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.response.training.CandidateTrainingRecordDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.training.TrainingMaterialResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.training.TrainingStatusSummaryDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.model.entity.training.CandidateTrainingRecord;
import com.event.recruitment.intelligent_recruitment_system.model.entity.training.TrainingMaterial;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.Jobs;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobApplicationRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.training.CandidateTrainingRecordRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.training.TrainingMaterialRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateTrainingService {

    private final CandidateTrainingRecordRepository trainingRecordRepository;
    private final CandidateRepository candidateRepository;
    private final TrainingMaterialRepository trainingMaterialRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final SecurityUtil securityUtil;

    /**
     * Record that a candidate has viewed a training material
     * Creates a record if none exists, or updates the existing record
     *
     * @param candidateId ID of the candidate
     * @param jobId ID of the job
     * @param materialId ID of the training material
     * @return Response containing the updated training record
     */
    @Transactional
    public Response<?> recordTrainingView(Long candidateId, Long jobId, Long materialId) {
        try {
            // Check if material exists and is enabled
            Optional<TrainingMaterial> materialOpt = trainingMaterialRepository.findById(materialId);
            if (materialOpt.isEmpty() || !materialOpt.get().getIsEnabled() || !materialOpt.get().getIsActive()) {
                return new Response<>(404, "Training material not found or not available", null);
            }

            // Find or create training record
            Optional<CandidateTrainingRecord> recordOpt =
                    trainingRecordRepository.findByCandidateIdAndJobIdAndTrainingMaterialId(
                            candidateId, jobId, materialId);

            CandidateTrainingRecord record;
            if (recordOpt.isEmpty()) {
                // Create new record
                record = new CandidateTrainingRecord(candidateId, jobId, materialId);
            } else {
                // Update existing record
                record = recordOpt.get();
            }

            // Record the view
            record.recordView();
            CandidateTrainingRecord savedRecord = trainingRecordRepository.save(record);

            CandidateTrainingRecordDTO recordDTO = new CandidateTrainingRecordDTO(savedRecord);

            // Get candidate name for the DTO
            Optional<Candidates> candidateOpt = candidateRepository.findById(candidateId);
            if (candidateOpt.isPresent()) {
                recordDTO.setCandidateName(candidateOpt.get().getName());
            }

            return new Response<>(200, "Training view recorded successfully", recordDTO);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Mark a training as completed for a candidate
     *
     * @param candidateId ID of the candidate
     * @param jobId ID of the job
     * @param materialId ID of the training material
     * @return Response containing the updated training record
     */
    @Transactional
    public Response<?> markTrainingCompleted(Long candidateId, Long jobId, Long materialId) {
        try {
            // Find training record
            Optional<CandidateTrainingRecord> recordOpt =
                    trainingRecordRepository.findByCandidateIdAndJobIdAndTrainingMaterialId(
                            candidateId, jobId, materialId);

            if (recordOpt.isEmpty()) {
                return new Response<>(404, "Training record not found. Candidate must view the training first.", null);
            }

            CandidateTrainingRecord record = recordOpt.get();

            // Mark as completed
            record.markAsCompleted();
            CandidateTrainingRecord savedRecord = trainingRecordRepository.save(record);

            CandidateTrainingRecordDTO recordDTO = new CandidateTrainingRecordDTO(savedRecord);

            // Get candidate name for the DTO
            Optional<Candidates> candidateOpt = candidateRepository.findById(candidateId);
            if (candidateOpt.isPresent()) {
                recordDTO.setCandidateName(candidateOpt.get().getName());
            }

            return new Response<>(200, "Training marked as completed successfully", recordDTO);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Get training records for all candidates of a specific job
     *
     * @param jobId ID of the job
     * @return Response containing the training records
     */
    public Response<?> getTrainingRecordsByJob(Long jobId) {
        try {
            // Get training records for the job
            List<CandidateTrainingRecord> records = trainingRecordRepository.findByJobId(jobId);

            if (records.isEmpty()) {
                return new Response<>(200, "No training records found for this job", new ArrayList<>());
            }

            // Get candidates to include names
            List<Long> candidateIds = records.stream()
                    .map(CandidateTrainingRecord::getCandidateId)
                    .distinct()
                    .collect(Collectors.toList());

            List<Candidates> candidates = candidateRepository.findAllById(candidateIds);
            Map<Long, String> candidateNames = new HashMap<>();
            for (Candidates candidate : candidates) {
                candidateNames.put(candidate.getId(), candidate.getName());
            }

            // Create DTOs with candidate names
            List<CandidateTrainingRecordDTO> recordDTOs = records.stream()
                    .map(record -> {
                        CandidateTrainingRecordDTO dto = new CandidateTrainingRecordDTO(record);
                        dto.setCandidateName(candidateNames.getOrDefault(record.getCandidateId(), "Unknown"));
                        return dto;
                    })
                    .collect(Collectors.toList());

            return new Response<>(200, "Training records retrieved successfully", recordDTOs);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Get a summary of training status for a specific job
     *
     * @param jobId ID of the job
     * @return Response containing the training status summary
     */
    public Response<?> getTrainingStatusSummary(Long jobId) {
        try {
            // Check if job exists
            Optional<Jobs> jobOpt = jobRepository.findById(jobId);
            if (jobOpt.isEmpty()) {
                return new Response<>(404, "Job not found", null);
            }

            Jobs job = jobOpt.get();

            // Get training records for the job
            List<CandidateTrainingRecord> records = trainingRecordRepository.findByJobId(jobId);

            // Count candidates for this job
            long totalCandidates = jobApplicationRepository.countByJobIdAndApplicationStatusIn(
                    jobId, List.of("HIRED", "ACCEPTED"));

            // Count viewed and completed
            int viewedCount = (int) records.stream()
                    .filter(r -> r.getFirstViewedAt() != null)
                    .count();

            int completedCount = (int) records.stream()
                    .filter(CandidateTrainingRecord::getIsCompleted)
                    .count();

            int notStartedCount = (int) (totalCandidates - viewedCount);

            // Calculate completion percentage
            double completionPercentage = totalCandidates > 0 ?
                    (completedCount * 100.0 / totalCandidates) : 0.0;

            TrainingStatusSummaryDTO summary = new TrainingStatusSummaryDTO(
                    jobId,
                    job.getTitle(),
                    (int) totalCandidates,
                    viewedCount,
                    completedCount,
                    notStartedCount,
                    completionPercentage
            );

            return new Response<>(200, "Training status summary retrieved successfully", summary);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Get training record for a specific candidate and training material
     *
     * @param candidateId ID of the candidate
     * @param jobId ID of the job
     * @param materialId ID of the training material
     * @return Response containing the training record
     */
    public Response<?> getTrainingRecord(Long candidateId, Long jobId, Long materialId) {
        try {
            // Find training record
            Optional<CandidateTrainingRecord> recordOpt =
                    trainingRecordRepository.findByCandidateIdAndJobIdAndTrainingMaterialId(
                            candidateId, jobId, materialId);

            if (recordOpt.isEmpty()) {
                return new Response<>(404, "Training record not found", null);
            }

            CandidateTrainingRecord record = recordOpt.get();
            CandidateTrainingRecordDTO recordDTO = new CandidateTrainingRecordDTO(record);

            // Get candidate name for the DTO
            Optional<Candidates> candidateOpt = candidateRepository.findById(candidateId);
            if (candidateOpt.isPresent()) {
                recordDTO.setCandidateName(candidateOpt.get().getName());
            }

            return new Response<>(200, "Training record retrieved successfully", recordDTO);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Get the single training material for a job and record view for the candidate
     *
     * @param candidateId ID of the candidate
     * @param jobId ID of the job
     * @return Response containing the training material and record
     */
    @Transactional
    public Response<?> getAndRecordTrainingMaterial(Long candidateId, Long jobId) {
        try {
            // Find the enabled training material for this job
            List<TrainingMaterial> materials = trainingMaterialRepository.findByJobIdAndIsActiveTrueAndIsEnabledTrue(jobId);

            if (materials.isEmpty()) {
                return new Response<>(404, "No training material found for this job", null);
            }

            // Since we're restricting to one training material per job, get the first one
            TrainingMaterial material = materials.get(0);

            // Record the view
            Optional<CandidateTrainingRecord> recordOpt =
                    trainingRecordRepository.findByCandidateIdAndJobIdAndTrainingMaterialId(
                            candidateId, jobId, material.getId());

            CandidateTrainingRecord record;
            if (recordOpt.isEmpty()) {
                // Create new record
                record = new CandidateTrainingRecord(candidateId, jobId, material.getId());
            } else {
                // Update existing record
                record = recordOpt.get();
            }

            // Record the view
            record.recordView();
            CandidateTrainingRecord savedRecord = trainingRecordRepository.save(record);

            // Get candidate name for the DTO
            CandidateTrainingRecordDTO recordDTO = new CandidateTrainingRecordDTO(savedRecord);
            Optional<Candidates> candidateOpt = candidateRepository.findById(candidateId);
            if (candidateOpt.isPresent()) {
                recordDTO.setCandidateName(candidateOpt.get().getName());
            }

            // Get job details
            Optional<Jobs> jobOpt = jobRepository.findById(jobId);
            Map<String, Object> jobInfo = new HashMap<>();

            if (jobOpt.isPresent()) {
                Jobs job = jobOpt.get();
                jobInfo.put("id", job.getId());
                jobInfo.put("title", job.getTitle());
                jobInfo.put("jobTitleType", job.getJobTitleType());
                jobInfo.put("jobDescription", job.getJobScope());
                jobInfo.put("requirements", job.getRequirements());
            }

            // Return the training material with the record info and job info
            Map<String, Object> response = new HashMap<>();
            response.put("material", new TrainingMaterialResponseDTO(material));
            response.put("record", recordDTO);
            response.put("job", jobInfo);

            return new Response<>(200, "Training material retrieved and view recorded", response);
        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Mark training as completed for a candidate's job
     *
     * @param candidateId ID of the candidate
     * @param jobId ID of the job
     * @return Response containing the updated training record
     */
    @Transactional
    public Response<?> markJobTrainingCompleted(Long candidateId, Long jobId) {
        try {
            // Find the training material for this job
            List<TrainingMaterial> materials = trainingMaterialRepository.findByJobIdAndIsActiveTrueAndIsEnabledTrue(jobId);

            if (materials.isEmpty()) {
                return new Response<>(404, "No training material found for this job", null);
            }

            // Get the first material (assuming one per job)
            TrainingMaterial material = materials.get(0);

            // Find training record
            Optional<CandidateTrainingRecord> recordOpt =
                    trainingRecordRepository.findByCandidateIdAndJobIdAndTrainingMaterialId(
                            candidateId, jobId, material.getId());

            if (recordOpt.isEmpty()) {
                return new Response<>(404, "Training record not found. Candidate must view the training first.", null);
            }

            CandidateTrainingRecord record = recordOpt.get();

            // Mark as completed
            record.markAsCompleted();
            CandidateTrainingRecord savedRecord = trainingRecordRepository.save(record);

            CandidateTrainingRecordDTO recordDTO = new CandidateTrainingRecordDTO(savedRecord);

            // Get candidate name for the DTO
            Optional<Candidates> candidateOpt = candidateRepository.findById(candidateId);
            if (candidateOpt.isPresent()) {
                recordDTO.setCandidateName(candidateOpt.get().getName());
            }

            return new Response<>(200, "Training marked as completed successfully", recordDTO);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Get training status for a candidate's job
     *
     * @param candidateId ID of the candidate
     * @param jobId ID of the job
     * @return Response containing the training status
     */
    public Response<?> getJobTrainingStatus(Long candidateId, Long jobId) {
        try {
            // Find the training material for this job
            List<TrainingMaterial> materials = trainingMaterialRepository.findByJobIdAndIsActiveTrueAndIsEnabledTrue(jobId);

            if (materials.isEmpty()) {
                return new Response<>(404, "No training material found for this job", null);
            }

            // Get the first material (assuming one per job)
            TrainingMaterial material = materials.get(0);

            // Find training record
            Optional<CandidateTrainingRecord> recordOpt =
                    trainingRecordRepository.findByCandidateIdAndJobIdAndTrainingMaterialId(
                            candidateId, jobId, material.getId());

            if (recordOpt.isEmpty()) {
                Map<String, Object> statusInfo = new HashMap<>();
                statusInfo.put("hasStarted", false);
                statusInfo.put("isCompleted", false);
                statusInfo.put("viewCount", 0);
                statusInfo.put("firstViewedAt", null);
                statusInfo.put("lastViewedAt", null);
                statusInfo.put("completionDate", null);

                return new Response<>(200, "Candidate has not started training yet", statusInfo);
            }

            CandidateTrainingRecord record = recordOpt.get();
            CandidateTrainingRecordDTO recordDTO = new CandidateTrainingRecordDTO(record);

            // Get candidate name for the DTO
            Optional<Candidates> candidateOpt = candidateRepository.findById(candidateId);
            if (candidateOpt.isPresent()) {
                recordDTO.setCandidateName(candidateOpt.get().getName());
            }

            Map<String, Object> statusInfo = new HashMap<>();
            statusInfo.put("hasStarted", record.getFirstViewedAt() != null);
            statusInfo.put("isCompleted", record.getIsCompleted());
            statusInfo.put("viewCount", record.getViewCount());
            statusInfo.put("firstViewedAt", record.getFirstViewedAt());
            statusInfo.put("lastViewedAt", record.getLastViewedAt());
            statusInfo.put("completionDate", record.getCompletionDate());
            statusInfo.put("trainingDetails", recordDTO);
            statusInfo.put("materialId", material.getId());

            String message = record.getIsCompleted() ?
                    "Candidate has completed the training" :
                    "Candidate has started but not completed the training";

            return new Response<>(200, message, statusInfo);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }
}