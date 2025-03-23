package com.event.recruitment.intelligent_recruitment_system.controller;

import com.event.recruitment.intelligent_recruitment_system.dto.RecruiterRegistrationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.Response;
import com.event.recruitment.intelligent_recruitment_system.service.RecruiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recruiter")
@Validated
public class RecruiterController {

    private final RecruiterService recruiterService;

    @Autowired
    public RecruiterController(RecruiterService recruiterService) {
        this.recruiterService = recruiterService;
    }


    @PostMapping("/register")
    public ResponseEntity<Response<?>> registerRecruiter(@RequestBody @Valid RecruiterRegistrationRequest recruiterRegistrationRequest) {
        try {
            Response<?> response = recruiterService.registerRecruiter(recruiterRegistrationRequest);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new Response<>(400, "Error registering recruiter", null));
        }
    }
}
