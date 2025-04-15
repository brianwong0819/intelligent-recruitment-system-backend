package com.event.recruitment.intelligent_recruitment_system.dto.request.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Email body is required")
    private String body;

    @Email(message = "From email must be valid")
    private String from;

    @NotBlank(message = "At least one recipient is required")
    private List<@Email(message = "To email must be valid") String> to;

    private List<@Email(message = "CC email must be valid") String> cc;

    private List<@Email(message = "BCC email must be valid") String> bcc;

    private Map<String, Object> templateVariables;

    private boolean isHtml;
}