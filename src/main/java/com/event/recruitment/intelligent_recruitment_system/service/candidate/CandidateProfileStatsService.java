package com.event.recruitment.intelligent_recruitment_system.service.candidate;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateProfileStatsDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.location.LocationResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.model.entity.location.Location;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateExperienceRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateWorkingPhotoRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateComcardRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import com.event.recruitment.intelligent_recruitment_system.service.location.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateProfileStatsService {

    private final CandidateRepository candidateRepository;
    private final CandidateExperienceRepository candidateExperienceRepository;
    private final CandidateWorkingPhotoRepository candidateWorkingPhotoRepository;
    private final CandidateComcardRepository candidateComcardRepository;
    private final LocationService locationService;
    private final SecurityUtil securityUtil;

    /**
     * Gets statistics about a candidate's profile including counts of experiences, photos, etc.
     *
     * @param candidateId The ID of the candidate to retrieve stats for. If null, uses the current logged-in candidate.
     * @return Response containing CandidateProfileStatsDTO with the counts
     */
    public Response<CandidateProfileStatsDTO> getCandidateProfileStats(Long candidateId) {
        try {
            // If candidateId is not provided, use the current logged-in user
            Long targetCandidateId = candidateId;
            if (targetCandidateId == null) {
                targetCandidateId = securityUtil.getCurrentCandidateId();
                if (targetCandidateId == null) {
                    return new Response<>(403, "Access denied or not logged in as a candidate", null);
                }
            }

            // Get the counts from repositories
            long experienceCount = candidateExperienceRepository.countByCandidateId(targetCandidateId);
            long workingPhotoCount = candidateWorkingPhotoRepository.countByCandidateId(targetCandidateId);
            long comcardCount = candidateComcardRepository.countByCandidateId(targetCandidateId);

            // Get candidate to check resume and preferred location
            Optional<Candidates> candidateOpt = candidateRepository.findById(targetCandidateId);

            if (candidateOpt.isEmpty()) {
                return new Response<>(404, "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();
            boolean hasResume = candidate.getResumeUrl() != null && !candidate.getResumeUrl().isEmpty();

            // Get preferred location (either from Location entity or via LocationService)
            String preferredLocation = null;
            if (candidate.getPreferredLocation() != null) {
                // Extract information from the Location entity
                Location location = candidate.getPreferredLocation();
                preferredLocation = location.getName();
            }

// Build the response DTO
            CandidateProfileStatsDTO statsDTO = new CandidateProfileStatsDTO(
                    targetCandidateId,
                    experienceCount,
                    hasResume,
                    workingPhotoCount,
                    comcardCount,
                    preferredLocation
            );

            return new Response<>(200, "Candidate profile statistics retrieved successfully", statsDTO);
        } catch (Exception e) {
            log.error("Error retrieving candidate profile statistics: {}", e.getMessage(), e);
            return new Response<>(500, "Error retrieving candidate profile statistics: " + e.getMessage(), null);
        }
    }
}