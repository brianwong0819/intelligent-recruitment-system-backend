// Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/service/training/TrainingQuizService.java
package com.event.recruitment.intelligent_recruitment_system.service.training;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.training.GenerateQuizRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingQuizService {

    private final ObjectMapper objectMapper;

    @Value("${gemini.ai.api-key}")
    private String geminiApiKey;

    @Value("${app.uploads.base-path:./}")
    private String baseDir;

    @Value("${gemini.ai.python-command:python3}")
    private String pythonCommand;

    @Value("${gemini.ai.timeout-seconds:60}")
    private long timeoutSeconds;

    /**
     * Generate a quiz from training material using Gemini AI
     */
    public Response<?> generateQuiz(GenerateQuizRequest request) {
        try {
            // Ensure we have the necessary data
            if (request.getTrainingMaterialUrl() == null) {
                return new Response<>(400, "Training material URL is required", null);
            }

            // Get job title and description if not provided
            String jobTitle = request.getJobTitle() != null ? request.getJobTitle() : "Event Staff";
            String jobDescription = request.getJobDescription();

            // Run Python script to generate quiz questions
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonCommand,
                    new File(baseDir, "scripts/generate_training_quiz.py").getAbsolutePath(),
                    "--api-key", geminiApiKey,
                    "--pdf-url", request.getTrainingMaterialUrl(),
                    "--job-title", jobTitle,
                    "--base-dir", baseDir
            );

            // Add job description if provided
            if (jobDescription != null && !jobDescription.trim().isEmpty()) {
                processBuilder.command().add("--job-description");
                processBuilder.command().add(jobDescription);
            }

            // Configure process
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Read the output with timeout
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Wait for the process to complete with timeout
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                // Process timed out, destroy it
                process.destroyForcibly();
                log.error("Python script timed out after {} seconds", timeoutSeconds);
                return new Response<>(500, "Script execution timed out", null);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Python script failed with exit code: {}. Output: {}", exitCode, output);
                return new Response<>(500, "Failed to generate quiz questions", null);
            }

            // Parse the JSON output
            JsonNode jsonNode = objectMapper.readTree(output.toString());

            // Check for errors
            if (jsonNode.has("error")) {
                log.error("Error generating quiz: {}", jsonNode.get("error").asText());
                return new Response<>(500, "Failed to generate quiz: " + jsonNode.get("error").asText(), null);
            }

            // Return the questions directly
            return new Response<>(200, "Quiz generated successfully", jsonNode);

        } catch (Exception e) {
            log.error("Error generating quiz", e);
            return new Response<>(500, "Failed to generate quiz: " + e.getMessage(), null);
        }
    }
}