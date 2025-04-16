// Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/controller/training/TrainingQuizController.java
package com.event.recruitment.intelligent_recruitment_system.controller.training;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.training.GenerateQuizRequest;
import com.event.recruitment.intelligent_recruitment_system.service.training.TrainingQuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
@Slf4j
public class TrainingQuizController {

    private final TrainingQuizService trainingQuizService;

    @PostMapping("/quiz/generate")
    public ResponseEntity<Response<?>> generateQuiz(@RequestBody GenerateQuizRequest request) {
        try {
            Response<?> response = trainingQuizService.generateQuiz(request);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            log.error("Error generating quiz", e);
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error generating quiz: " + e.getMessage(), null)
            );
        }
    }
}