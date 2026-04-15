package com.makemytour.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends transactional e-mails (password-reset links, etc.).
 * Requires MAIL_USERNAME and MAIL_PASSWORD environment variables.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Sends a password-reset link to the given e-mail address.
     *
     * @param toEmail recipient e-mail
     * @param token   UUID password-reset token
     * @throws IllegalStateException if the mail cannot be sent
     */
    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("MakeMyTour – Reset Your Password");
        message.setText(
            "Hello,\n\n"
            + "We received a request to reset the password for your MakeMyTour account.\n\n"
            + "Click the link below to choose a new password (valid for 1 hour):\n"
            + resetLink + "\n\n"
            + "If you did not request a password reset, you can safely ignore this email.\n\n"
            + "– The MakeMyTour Team"
        );

        try {
            mailSender.send(message);
            log.info("Password-reset email sent to {}", toEmail);
        } catch (MailException ex) {
            log.error("Failed to send password-reset email to {}: {}", toEmail, ex.getMessage());
            throw new IllegalStateException(
                "Could not send password-reset email. Please try again later.", ex);
        }
    }
}
