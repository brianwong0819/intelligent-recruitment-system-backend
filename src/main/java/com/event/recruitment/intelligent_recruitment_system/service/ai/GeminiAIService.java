package com.event.recruitment.intelligent_recruitment_system.service.ai;

import com.event.recruitment.intelligent_recruitment_system.dto.ai.CandidateAIEvaluationDataDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.ai.GeminiAIResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service to communicate with the Gemini AI through Python script
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAIService {

    private final ObjectMapper objectMapper;

    @Value("${gemini.ai.api-key}")
    private String geminiApiKey;

    @Value("${gemini.ai.script-path}")
    private String pythonScriptPath;

    @Value("${gemini.ai.python-command:python3}")
    private String pythonCommand;

    @Value("${gemini.ai.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${app.uploads.base-path:./}")
    private String uploadsBasePath;

    /**
     * Evaluate a candidate using Gemini AI
     *
     * @param evaluationData Data collected for AI evaluation
     * @return Gemini AI response DTO with scores and feedback
     */
    public GeminiAIResponseDTO evaluateCandidate(CandidateAIEvaluationDataDTO evaluationData) {
        try {
            log.info("Preparing to evaluate candidate with Gemini AI");

            // Create temporary files for input/output
            Path inputFile = Files.createTempFile("candidate_data_", ".json");
            Path outputFile = Files.createTempFile("ai_response_", ".json");

            try {
                // Write candidate data to input file using UTF-8 encoding
                Files.write(inputFile,
                        objectMapper.writeValueAsString(evaluationData).getBytes(StandardCharsets.UTF_8));
                log.debug("Wrote candidate data to {}", inputFile);

                // Determine base directory for resume files
                File baseDir = new File(uploadsBasePath);
                String baseDirPath = baseDir.getAbsolutePath();
                log.debug("Using base directory for resumes: {}", baseDirPath);

                // Prepare command to run Python script
                List<String> command = new ArrayList<>();
                command.add(pythonCommand);
                command.add(pythonScriptPath);
                command.add("--api-key");
                command.add(geminiApiKey);
                command.add("--input");
                command.add(inputFile.toString());
                command.add("--output");
                command.add(outputFile.toString());
                command.add("--base-dir");
                command.add(baseDirPath);

                log.info("Executing Python script for Gemini AI evaluation");
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();

                // Read output for logging purposes
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                // Wait for the process to complete with timeout
                boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
                if (!completed) {
                    process.destroyForcibly();
                    log.error("Python script execution timed out after {} seconds", timeoutSeconds);
                    return createErrorResponse("Process timed out", output.toString());
                }

                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    log.error("Python script execution failed with exit code {}: {}", exitCode, output);
                    return createErrorResponse("Process exited with code " + exitCode, output.toString());
                }

                log.info("Python script executed successfully");

                // Read the output file
                File resultFile = outputFile.toFile();
                if (!resultFile.exists() || resultFile.length() == 0) {
                    log.error("Output file is empty or doesn't exist");
                    return createErrorResponse("Output file is empty or doesn't exist", output.toString());
                }

                // Read the file with UTF-8 encoding
                String jsonContent = Files.readString(outputFile, StandardCharsets.UTF_8);
                GeminiAIResponseDTO response = objectMapper.readValue(jsonContent, GeminiAIResponseDTO.class);

                if (response.hasError()) {
                    log.error("Gemini AI returned an error: {}", response.getError());
                } else {
                    log.info("Successfully received Gemini AI evaluation");
                }

                return response;

            } finally {
                // Clean up temporary files
                try {
                    Files.deleteIfExists(inputFile);
                    Files.deleteIfExists(outputFile);
                } catch (Exception e) {
                    log.warn("Failed to delete temporary files: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error during Gemini AI evaluation: {}", e.getMessage(), e);
            return createErrorResponse("Exception: " + e.getMessage(), null);
        }
    }

    /**
     * Create an error response DTO
     *
     * @param errorMessage The error message
     * @param rawOutput Raw output from the process
     * @return GeminiAIResponseDTO with error information
     */
    private GeminiAIResponseDTO createErrorResponse(String errorMessage, String rawOutput) {
        GeminiAIResponseDTO response = new GeminiAIResponseDTO();
        response.setError(errorMessage);
        response.setRawResponse(rawOutput);

        // Set fallback/default scores in case of error
        response.setExperienceScore(5.0);
        response.setSkillsScore(5.0);
        response.setLocationScore(5.0);
        response.setAvailabilityScore(5.0);
        response.setResumeScore(5.0);
        response.setReputationScore(5.0);
        response.setAiModelScore(5.0);
        response.setFinalScore(5.0);
        response.setFeedback("Error during AI evaluation: " + errorMessage);

        return response;
    }
}