package com.event.recruitment.intelligent_recruitment_system.controller;

import com.event.recruitment.intelligent_recruitment_system.dto.CandidateRegistrationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.Response;
import com.event.recruitment.intelligent_recruitment_system.service.CandidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/candidate")
@Validated
public class CandidateController {

    private final CandidateService candidateService;

    @Autowired
    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    // 注册候选人
    @PostMapping("/register")
    public ResponseEntity<Response<?>> registerCandidate(@RequestBody @Valid CandidateRegistrationRequest candidateRegistrationRequest) {
        try {
            Response<?> response = candidateService.registerCandidate(candidateRegistrationRequest);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new Response<>(400, "Error registering candidate", null));
        }
    }

    // 更新候选人信息
    @PutMapping("/{id}/update")
    public ResponseEntity<Response<?>> updateCandidate(@PathVariable("id") Long id,
                                                       @RequestBody @Valid CandidateRegistrationRequest candidateRegistrationRequest) {
        try {
            Response<?> response = candidateService.updateCandidate(id, candidateRegistrationRequest);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new Response<>(400, "Error updating candidate", null));
        }
    }
}
