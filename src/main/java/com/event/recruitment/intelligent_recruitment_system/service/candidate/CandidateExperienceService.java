package com.event.recruitment.intelligent_recruitment_system.service.candidate;

import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateExperienceDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.request.candidate.CandidateExperienceRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateExperience;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateExperienceRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateExperienceService {

    private final CandidateExperienceRepository experienceRepository;
    private final CandidateRepository candidateRepository;
    private final SecurityUtil securityUtil;

    /**
     * Add a new experience for the logged-in candidate
     */
    public Response<?> addExperience(CandidateExperienceRequest request) {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

            // Find candidate by username
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);
            if (candidateOpt.isEmpty()) {
                return new Response<>(404, "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Create and save new experience
            CandidateExperience experience = new CandidateExperience(
                    candidate.getId(),
                    request.getJobType(),
                    request.getExperienceText()
            );

            CandidateExperience savedExperience = experienceRepository.save(experience);
            CandidateExperienceDTO experienceDTO = new CandidateExperienceDTO(savedExperience);

            return new Response<>(201, "Experience added successfully", experienceDTO);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Get all experiences for the logged-in candidate
     */
    public Response<?> getAllExperiences() {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

            // Find candidate by username
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);
            if (candidateOpt.isEmpty()) {
                return new Response<>(404, "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Get experiences
            List<CandidateExperience> experiences = experienceRepository.findByCandidateId(candidate.getId());
            List<CandidateExperienceDTO> experienceDTOs = experiences.stream()
                    .map(CandidateExperienceDTO::new)
                    .collect(Collectors.toList());

            return new Response<>(200, "Experiences retrieved successfully", experienceDTOs);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Get a specific experience by ID for the logged-in candidate
     */
    public Response<?> getExperienceById(Long experienceId) {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

            // Find candidate by username
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);
            if (candidateOpt.isEmpty()) {
                return new Response<>(404, "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Find experience
            Optional<CandidateExperience> experienceOpt = experienceRepository.findByIdAndCandidateId(experienceId, candidate.getId());
            if (experienceOpt.isEmpty()) {
                return new Response<>(404, "Experience not found", null);
            }

            CandidateExperienceDTO experienceDTO = new CandidateExperienceDTO(experienceOpt.get());

            return new Response<>(200, "Experience retrieved successfully", experienceDTO);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Update an existing experience for the logged-in candidate
     */
    public Response<?> updateExperience(Long experienceId, CandidateExperienceRequest request) {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

            // Find candidate by username
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);
            if (candidateOpt.isEmpty()) {
                return new Response<>(404, "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Find experience
            Optional<CandidateExperience> experienceOpt = experienceRepository.findByIdAndCandidateId(experienceId, candidate.getId());
            if (experienceOpt.isEmpty()) {
                return new Response<>(404, "Experience not found", null);
            }

            CandidateExperience experience = experienceOpt.get();

            // Update experience
            experience.setJobType(request.getJobType());
            experience.setExperienceText(request.getExperienceText());

            CandidateExperience updatedExperience = experienceRepository.save(experience);
            CandidateExperienceDTO experienceDTO = new CandidateExperienceDTO(updatedExperience);

            return new Response<>(200, "Experience updated successfully", experienceDTO);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }

    /**
     * Delete an experience for the logged-in candidate
     */
    @Transactional
    public Response<?> deleteExperience(Long experienceId) {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

            // Find candidate by username
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);
            if (candidateOpt.isEmpty()) {
                return new Response<>(404, "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Find experience
            Optional<CandidateExperience> experienceOpt = experienceRepository.findByIdAndCandidateId(experienceId, candidate.getId());
            if (experienceOpt.isEmpty()) {
                return new Response<>(404, "Experience not found", null);
            }

            // Delete experience
            experienceRepository.deleteByIdAndCandidateId(experienceId, candidate.getId());

            return new Response<>(200, "Experience deleted successfully", null);

        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }
}