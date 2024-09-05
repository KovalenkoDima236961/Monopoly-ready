package com.dimon.catanbackend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for sending emails using the {@link JavaMailSender} API.
 * This service constructs and sends HTML emails to recipients with the provided subject and content.
 *
 * Annotations used:
 * - {@link Service} to mark this class as a Spring service component.
 * - {@link Autowired} to inject the {@link JavaMailSender} dependency.
 *
 * Methods:
 * - {@code sendEmail}: Sends an email with the specified recipient, subject, and HTML content.
 *
 * Exceptions:
 * - Throws {@link MessagingException} if there is an issue creating or sending the email.
 *
 * Example usage:
 * <pre>
 * {@code
 * emailSenderService.sendEmail("recipient@example.com", "Subject", "<h1>HTML Content</h1>");
 * }
 * </pre>
 *
 * @see JavaMailSender
 * @see MimeMessageHelper
 * @see MimeMessage
 * @see MessagingException
 *
 */
@Service
public class EmailSenderService {

    private final JavaMailSender mailSender;

    /**
     * Constructor for {@code EmailSenderService}.
     *
     * @param mailSender the {@link JavaMailSender} used to send emails
     */
    @Autowired
    public EmailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an email to the specified recipient with the given subject and HTML content.
     *
     * @param to the recipient's email address
     * @param subject the subject of the email
     * @param htmlContent the HTML content of the email
     * @throws MessagingException if an error occurs while creating or sending the email
     */
    public void sendEmail(String to, String subject, String htmlContent) throws MessagingException{
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

        helper.setFrom("kovalenkodima581@gmail.com");
        helper.setText(htmlContent, true);
        helper.setTo(to);
        helper.setSubject(subject);

        mailSender.send(mimeMessage);
    }

}
