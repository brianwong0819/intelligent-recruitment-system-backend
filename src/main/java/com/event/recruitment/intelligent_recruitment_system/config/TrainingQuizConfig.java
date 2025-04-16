// Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/config/TrainingQuizConfig.java
package com.event.recruitment.intelligent_recruitment_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Configuration for Training Quiz integration with Gemini AI
 */
@Configuration
@PropertySource("classpath:training-quiz.properties")
@ConfigurationProperties(prefix = "training.quiz")
public class TrainingQuizConfig {
    private String apiKey;
    private String scriptPath;
    private String pythonCommand;
    private String uploadsBasePath;
    private int timeoutSeconds;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    public String getPythonCommand() {
        return pythonCommand;
    }

    public void setPythonCommand(String pythonCommand) {
        this.pythonCommand = pythonCommand;
    }

    public String getUploadsBasePath() {
        return uploadsBasePath;
    }

    public void setUploadsBasePath(String uploadsBasePath) {
        this.uploadsBasePath = uploadsBasePath;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}