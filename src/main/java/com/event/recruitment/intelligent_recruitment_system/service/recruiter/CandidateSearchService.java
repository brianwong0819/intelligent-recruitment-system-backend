package com.event.recruitment.intelligent_recruitment_system.service.recruiter;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.recruiter.CandidateSearchRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateSearchResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.common.PagedResponse;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateExperience;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateExperienceRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateSpecifications;
import com.event.recruitment.intelligent_recruitment_system.util.CandidateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateSearchService {

    private final CandidateRepository candidateRepository;
    private final CandidateExperienceRepository candidateExperienceRepository;
    private final CandidateMapper candidateMapper;

    public Response<PagedResponse<CandidateSearchResponseDTO>> searchCandidates(CandidateSearchRequest request) {
        try {
            // Create pageable with sorting
            Pageable pageable;
            if (request.getSortBy() != null && !request.getSortBy().isEmpty()) {
                Sort sort = request.getSortDesc() != null && request.getSortDesc()
                        ? Sort.by(request.getSortBy()).descending()
                        : Sort.by(request.getSortBy()).ascending();
                pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
            } else {
                // Default sort by name ascending
                pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by("name").ascending());
            }

            // Build specifications for filtering
            Specification<Candidates> spec = Specification.where(CandidateSpecifications.isSearchable());

            // Apply filters if provided
            if (request.getAvailability() != null) {
                spec = spec.and(CandidateSpecifications.hasAvailability(request.getAvailability()));
            }

            if (request.getEmploymentStatus() != null) {
                spec = spec.and(CandidateSpecifications.hasEmploymentStatus(request.getEmploymentStatus()));
            }

            if (request.getGender() != null) {
                spec = spec.and(CandidateSpecifications.hasGender(request.getGender()));
            }

            if (request.getEthnicity() != null) {
                spec = spec.and(CandidateSpecifications.hasEthnicity(request.getEthnicity()));
            }

            if (request.getMinAge() != null || request.getMaxAge() != null) {
                spec = spec.and(CandidateSpecifications.hasAgeRange(request.getMinAge(), request.getMaxAge()));
            }

            if (request.getLanguages() != null && !request.getLanguages().isEmpty()) {
                spec = spec.and(CandidateSpecifications.hasAnyLanguages(request.getLanguages()));
            }

            if (request.getMinExperience() != null) {
                spec = spec.and(CandidateSpecifications.hasMinimumExperiences(request.getMinExperience()));
            }

            if (request.getAvailableDates() != null && !request.getAvailableDates().isEmpty()) {
                spec = spec.and(CandidateSpecifications.isAvailableOnDates(request.getAvailableDates()));
            }

            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                spec = spec.and(CandidateSpecifications.containsKeyword(request.getKeyword()));
            }

            // Execute the query with all filters applied
            Page<Candidates> candidatesPage = candidateRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<CandidateSearchResponseDTO> candidateDTOs = candidatesPage.getContent().stream()
                    .map(candidate -> {
                        List<CandidateExperience> experiences =
                                candidateExperienceRepository.findByCandidateId(candidate.getId());
                        return candidateMapper.toSearchResponseDTO(candidate, experiences);
                    })
                    .collect(Collectors.toList());

            // Create paged response
            PagedResponse<CandidateSearchResponseDTO> pagedResponse = new PagedResponse<>();
            pagedResponse.setContent(candidateDTOs);
            pagedResponse.setPage(candidatesPage.getNumber());
            pagedResponse.setSize(candidatesPage.getSize());
            pagedResponse.setTotalElements(candidatesPage.getTotalElements());
            pagedResponse.setTotalPages(candidatesPage.getTotalPages());
            pagedResponse.setLast(candidatesPage.isLast());

            return new Response<>(HttpStatus.OK.value(), "Candidates retrieved successfully", pagedResponse);
        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error searching candidates: " + e.getMessage(), null);
        }
    }
}