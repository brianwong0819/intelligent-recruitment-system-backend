package com.event.recruitment.intelligent_recruitment_system.service.email;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String defaultFromEmail;

    private static final String SENDER_NAME = "Crew Connect";

    /**
     * Send a simple email
     *
     * @param to recipient email address
     * @param subject email subject
     * @param body email body content
     * @param isHtml whether the content is HTML
     * @return true if sent successfully, false otherwise
     */
    public boolean sendSimpleEmail(String to, String subject, String body, boolean isHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setFrom(new InternetAddress(defaultFromEmail, SENDER_NAME));
            helper.setSubject(subject);
            helper.setText(body, isHtml);

            mailSender.send(message);
            return true;

        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send email using a template
     *
     * @param to recipient email address
     * @param subject email subject
     * @param templateName name of the template
     * @param variables template variables
     * @return true if sent successfully, false otherwise
     */
    public boolean sendTemplateEmail(String to, String subject,
                                     String templateName, Map<String, Object> variables) {
        try {
            // Create Thymeleaf context and add variables
            Context context = new Context();
            if (variables != null) {
                variables.forEach(context::setVariable);
            }

            // Process the template
            String emailContent = templateEngine.process(templateName, context);

            // Send the email
            return sendSimpleEmail(to, subject, emailContent, true);

        } catch (Exception e) {
            log.error("Failed to send template email: {}", e.getMessage(), e);
            return false;
        }
    }
}
