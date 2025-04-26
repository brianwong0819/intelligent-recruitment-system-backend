package com.event.recruitment.intelligent_recruitment_system.service.candidate;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateExperienceDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateProfileDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateSummaryDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateAvailabilityDate;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateExperience;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateAvailabilityDateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateComcardRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateExperienceRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateWorkingPhotoRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import com.event.recruitment.intelligent_recruitment_system.util.CandidateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for candidate profile operations for recruiters
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateProfileService {

    private final CandidateRepository candidateRepository;
    private final CandidateExperienceRepository experienceRepository;
    private final CandidateWorkingPhotoRepository workingPhotoRepository;
    private final CandidateComcardRepository comcardRepository;
    private final CandidateAvailabilityDateRepository availabilityDateRepository;
    private final CandidateMapper candidateMapper;
    private final SecurityUtil securityUtil;

    /**
     * Retrieves detailed profile information for a specific candidate
     *
     * @param candidateId The ID of the candidate to retrieve
     * @return Response containing the candidate profile data
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('RECRUITER')")
    public Response<CandidateProfileDTO> getCandidateProfile(Long candidateId) {
        try {
            log.debug("Fetching candidate profile for ID: {}", candidateId);

            Optional<Candidates> candidateOpt = candidateRepository.findById(candidateId);
            if (candidateOpt.isEmpty() || Boolean.TRUE.equals(candidateOpt.get().getIsDeleted())) {
                log.warn("Candidate not found or is deleted: {}", candidateId);
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();
            log.debug("Found candidate: {}", candidate.getName());

            CandidateProfileDTO profileDTO = candidateMapper.toCandidateProfileDTO(candidate);

            // Fetch and map experiences
            log.debug("Fetching experiences for candidate ID: {}", candidate.getId());
            List<CandidateExperience> experiences = experienceRepository.findByCandidateId(candidate.getId());
            List<CandidateExperienceDTO> experienceDTOs = experiences.stream()
                    .map(candidateMapper::toExperienceDTO)
                    .collect(Collectors.toList());
            profileDTO.setExperiences(experienceDTOs);
            log.debug("Found {} experiences for candidate", experiences.size());

            // Fetch and map working photos
            log.debug("Fetching working photos for candidate ID: {}", candidate.getId());
            profileDTO.setWorkingPhotos(workingPhotoRepository.findByCandidateId(candidate.getId()).stream()
                    .map(candidateMapper::toWorkingPhotoDTO)
                    .collect(Collectors.toList()));

            // Fetch and map comcards - THIS IS WHERE THE ERROR OCCURS
            // Change from findByCandidateId to findAllByCandidateId
            log.debug("Fetching comcards for candidate ID: {}", candidate.getId());
            profileDTO.setComcards(comcardRepository.findAllByCandidateId(candidate.getId()).stream()
                    .map(candidateMapper::toComcardDTO)
                    .collect(Collectors.toList()));

            // Fetch and map availability dates
            log.debug("Fetching availability dates for candidate ID: {}", candidate.getId());
            List<CandidateAvailabilityDate> availabilityDates = availabilityDateRepository.findByCandidateId(candidate.getId());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            profileDTO.setAvailabilityDates(availabilityDates.stream()
                    .map(date -> date.getAvailableDate().format(formatter))
                    .collect(Collectors.toList()));

            log.info("Successfully retrieved candidate profile for ID: {}", candidateId);
            return new Response<>(HttpStatus.OK.value(), "Candidate profile retrieved successfully", profileDTO);
        } catch (Exception e) {
            log.error("Failed to retrieve candidate profile for ID: " + candidateId, e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to retrieve candidate profile: " + e.getMessage(), null);
        }
    }

    /**
     * Retrieves a paginated list of all candidates with basic information
     *
     * @param pageable Pagination parameters
     * @return Response containing a page of candidate summaries
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('RECRUITER')")
    public Response<Page<CandidateSummaryDTO>> getAllCandidates(Pageable pageable) {
        try {
            log.debug("Fetching all candidates with pagination: {}", pageable);

            // Get paginated list of non-deleted candidates
            Page<Candidates> candidatesPage = candidateRepository.findByIsDeletedFalse(pageable);
            log.debug("Found {} candidates in page {} of size {}",
                    candidatesPage.getNumberOfElements(),
                    pageable.getPageNumber(),
                    pageable.getPageSize());

            // Map to DTOs
            Page<CandidateSummaryDTO> candidateSummaries = candidatesPage.map(candidate -> {
                CandidateSummaryDTO summaryDTO = candidateMapper.toCandidateSummaryDTO(candidate);

                // Get experience tags from experience descriptions
                List<CandidateExperience> experiences = experienceRepository.findByCandidateId(candidate.getId());
                List<String> experienceTags = experiences.stream()
                        .filter(exp -> exp.getJobType() != null)
                        .map(exp -> exp.getJobType().name())
                        .distinct()
                        .collect(Collectors.toList());
                summaryDTO.setExperienceTags(experienceTags);

                return summaryDTO;
            });

            log.info("Successfully retrieved paginated list of candidates");
            return new Response<>(HttpStatus.OK.value(),
                    "Candidates retrieved successfully", candidateSummaries);
        } catch (Exception e) {
            log.error("Failed to retrieve candidates", e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to retrieve candidates: " + e.getMessage(), null);
        }
    }
}