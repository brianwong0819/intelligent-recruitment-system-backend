package com.event.recruitment.intelligent_recruitment_system.service.training;

import com.event.recruitment.intelligent_recruitment_system.config.TrainingQuizConfig;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.training.GenerateQuizRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingQuizService {

    private final ObjectMapper objectMapper;
    private final TrainingQuizConfig quizConfig;

    public Response<?> generateQuiz(GenerateQuizRequest request) {
        try {
            // Create temporary file for request data
            Path tempInputFile = Files.createTempFile("quiz_request_", ".json");

            // Write request data to temp file
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("trainingUrl", request.getTrainingMaterialUrl());
            requestData.put("jobTitle", request.getJobTitle());
            requestData.put("jobDescription", request.getJobDescription());
            Files.writeString(tempInputFile, objectMapper.writeValueAsString(requestData));

            // Create temp file for output
            Path tempOutputFile = Files.createTempFile("quiz_result_", ".json");

            // Prepare command to run Python script
            List<String> command = new ArrayList<>();
            command.add(quizConfig.getPythonCommand());
            command.add(quizConfig.getScriptPath());
            command.add("--api-key");
            command.add(quizConfig.getApiKey());
            command.add("--pdf-url");
            command.add(request.getTrainingMaterialUrl());
            command.add("--job-title");
            command.add(request.getJobTitle());
            command.add("--output");
            command.add(tempOutputFile.toString());
            command.add("--base-dir");
            command.add(quizConfig.getUploadsBasePath());

            if (request.getJobDescription() != null && !request.getJobDescription().isEmpty()) {
                command.add("--job-description");
                command.add(request.getJobDescription());
            }

            log.info("Executing command: {}", String.join(" ", command));

            // Execute the command
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            // Read any error output from the script
            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }

            // Capture standard output for logging
            StringBuilder stdOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdOutput.append(line).append("\n");
                }
            }

            // Wait for process to complete with timeout
            int timeoutSeconds = quizConfig.getTimeoutSeconds() > 0 ?
                    quizConfig.getTimeoutSeconds() : 180; // Default to 3 minutes if not set

            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                process.destroy();
                log.error("Python script timed out after {} seconds", timeoutSeconds);
                return new Response<>(500, "Quiz generation timed out after " + timeoutSeconds + " seconds", null);
            }

            // Check if process completed successfully
            if (process.exitValue() != 0) {
                log.error("Python script failed with exit code: {}", process.exitValue());
                log.error("Error output: {}", errorOutput);
                log.error("Standard output: {}", stdOutput);
                return new Response<>(500, "Quiz generation failed with exit code: " + process.exitValue() +
                        ". Error: " + errorOutput.toString().trim(), null);
            }

            // Log standard output
            log.debug("Python script output: {}", stdOutput);

            // Read the output file
            if (!Files.exists(tempOutputFile) || Files.size(tempOutputFile) == 0) {
                log.error("Output file is empty or does not exist");
                return new Response<>(500, "Quiz generation produced no output", null);
            }

            String jsonOutput = Files.readString(tempOutputFile);
            log.debug("JSON Output: {}", jsonOutput);

            // Parse JSON while handling any potential issues
            JsonNode resultNode;
            try {
                // The critical step - ensure we have valid JSON
                resultNode = objectMapper.readTree(jsonOutput);

                // Check if there's an error in the result
                if (resultNode.has("error")) {
                    return new Response<>(500, "Failed to generate quiz: " + resultNode.get("error").asText(), null);
                }

                // Return the questions
                return new Response<>(200, "Quiz generated successfully", resultNode);
            } catch (Exception e) {
                log.error("JSON parsing error: {}", e.getMessage());
                log.error("Raw output: {}", jsonOutput);

                // Try to extract JSON from the text - find the first '{' and last '}'
                int startIdx = jsonOutput.indexOf('{');
                int endIdx = jsonOutput.lastIndexOf('}') + 1;

                if (startIdx >= 0 && endIdx > startIdx) {
                    String jsonStr = jsonOutput.substring(startIdx, endIdx);
                    try {
                        resultNode = objectMapper.readTree(jsonStr);
                        return new Response<>(200, "Quiz generated successfully (extracted from output)", resultNode);
                    } catch (Exception e2) {
                        log.error("Failed to extract valid JSON: {}", e2.getMessage());
                    }
                }

                return new Response<>(500, "Failed to parse quiz result: " + e.getMessage(), null);
            } finally {
                // Clean up temp files
                Files.deleteIfExists(tempInputFile);
                Files.deleteIfExists(tempOutputFile);
            }
        } catch (Exception e) {
            log.error("Error generating quiz", e);
            return new Response<>(500, "Failed to generate quiz: " + e.getMessage(), null);
        }
    }
}