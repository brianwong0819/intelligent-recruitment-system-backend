package com.event.recruitment.intelligent_recruitment_system.controller.recruiter;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.recruiter.CandidateSearchRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateSearchResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.common.PagedResponse;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Availability;
import com.event.recruitment.intelligent_recruitment_system.model.enums.EmploymentStatus;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Gender;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Language;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Race;
import com.event.recruitment.intelligent_recruitment_system.service.recruiter.CandidateSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recruiters/candidates")
@RequiredArgsConstructor
public class CandidateSearchController {

    private final CandidateSearchService candidateSearchService;

    /**
     * Search for candidates with filters and sorting
     * @param page Page number (starts from 0)
     * @param size Page size
     * @param sortBy Field to sort by
     * @param sortDesc Sort direction (true for descending)
     * @param availability Filter by availability status
     * @param minAge Filter by minimum age
     * @param maxAge Filter by maximum age
     * @param employmentStatus Filter by employment status
     * @param ethnicity Filter by ethnicity (race)
     * @param languages Filter by languages
     * @param gender Filter by gender
     * @param minExperience Filter by minimum number of experiences
     * @param availableDates Filter by available dates
     * @param keyword Search keyword
     * @return Paged response with candidate information
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<PagedResponse<CandidateSearchResponseDTO>>> searchCandidates(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Boolean sortDesc,
            @RequestParam(required = false) String availability,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String employmentStatus,
            @RequestParam(required = false) String ethnicity,
            @RequestParam(required = false) List<String> languages,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Integer minExperience,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) List<LocalDate> availableDates,
            @RequestParam(required = false) String keyword
    ) {
        try {
            // Convert string parameters to enums when necessary
            Availability availabilityEnum = availability != null ?
                    Availability.valueOf(availability) : null;

            EmploymentStatus employmentStatusEnum = employmentStatus != null ?
                    EmploymentStatus.valueOf(employmentStatus) : null;

            Race ethnicityEnum = ethnicity != null ?
                    Race.valueOf(ethnicity) : null;

            Gender genderEnum = gender != null ?
                    Gender.valueOf(gender) : null;

            // Convert string language values to Language enum
            List<Language> languageEnums = null;
            if (languages != null && !languages.isEmpty()) {
                languageEnums = languages.stream()
                        .map(lang -> Language.valueOf(lang))
                        .collect(Collectors.toList());
            }

            // Build the search request from parameters
            CandidateSearchRequest searchRequest = CandidateSearchRequest.builder()
                    .page(page)
                    .size(size)
                    .sortBy(sortBy)
                    .sortDesc(sortDesc)
                    .availability(availabilityEnum)
                    .minAge(minAge)
                    .maxAge(maxAge)
                    .employmentStatus(employmentStatusEnum)
                    .ethnicity(ethnicityEnum)
                    .languages(languageEnums)
                    .gender(genderEnum)
                    .minExperience(minExperience)
                    .availableDates(availableDates)
                    .keyword(keyword)
                    .build();

            Response<PagedResponse<CandidateSearchResponseDTO>> response = candidateSearchService.searchCandidates(searchRequest);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error searching candidates: " + e.getMessage(), null));
        }
    }

    /**
     * Simplified search for candidates (POST method for complex requests)
     * @param searchRequest The search request object
     * @return Paged response with candidate information
     */
    @PostMapping("/search")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Response<PagedResponse<CandidateSearchResponseDTO>>> searchCandidatesPost(
            @RequestBody CandidateSearchRequest searchRequest
    ) {
        try {
            Response<PagedResponse<CandidateSearchResponseDTO>> response = candidateSearchService.searchCandidates(searchRequest);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error searching candidates: " + e.getMessage(), null));
        }
    }
}