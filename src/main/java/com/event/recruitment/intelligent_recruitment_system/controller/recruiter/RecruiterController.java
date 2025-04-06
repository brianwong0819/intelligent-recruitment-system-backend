package com.event.recruitment.intelligent_recruitment_system.controller.recruiter;

import com.event.recruitment.intelligent_recruitment_system.dto.request.auth.RecruiterRegistrationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.service.recruiter.RecruiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recruiter")
@Validated
@RequiredArgsConstructor
public class RecruiterController {

    private final RecruiterService recruiterService;

    @PostMapping("/register")
    public ResponseEntity<Response<?>> registerRecruiter(@RequestBody @Valid RecruiterRegistrationRequest recruiterRegistrationRequest) {
        try {
            Response<?> response = recruiterService.registerRecruiter(recruiterRegistrationRequest);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error registering recruiter: " + e.getMessage(), null));
        }
    }
}