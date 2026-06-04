package com.example.eventsphere.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final String mailUsername;

    @Autowired
    public EmailService(JavaMailSender javaMailSender, @Value("${spring.mail.username}") String mailUsername) {
        this.javaMailSender = javaMailSender;
        this.mailUsername = mailUsername;
    }

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setFrom(mailUsername);
            message.setSubject("Your EventSphere Verification Code");
            message.setText("Welcome to EventSphere!\n\n" +
                    "Your 6-digit verification code is: " + otp + "\n\n" +
                    "This code will expire in 10 minutes. Please do not share it with anyone.");

            javaMailSender.send(message);
            log.info("OTP email successfully sent to {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", toEmail, e);
            throw new RuntimeException("Could not send verification email. Please check your email settings.");
        }
    }

    public void sendPasswordResetEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setFrom(mailUsername);
            message.setSubject("EventSphere - Password Reset Request");
            message.setText("We received a request to reset your password.\n\n" +
                    "Your 6-digit password reset code is: " + otp + "\n\n" +
                    "This code will expire in 10 minutes. If you did not request this, please ignore this email.");

            javaMailSender.send(message);
            log.info("Password reset OTP email successfully sent to {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send Password Reset email to {}", toEmail, e);
            throw new RuntimeException("Could not send verification email. Please try again later.");
        }
    }

    public void sendEmail(String to, String subject, String body) {
        try {

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            helper.setFrom(mailUsername); // Optional, usually inferred from properties

            javaMailSender.send(message);
            log.info("Email successfully sent to {}", to);
        } catch (Exception e) {
            log.error("Exception while sending email to {}", to, e);
        }
    }
    public void sendBccEmail(List<String> bccEmails, String subject, String body) {
        try {
            if (bccEmails == null || bccEmails.isEmpty()) {
                log.warn("BCC email list is empty. Skipping send.");
                throw new RuntimeException("BCC email list is empty. Skipping send.");
            }
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setSubject(subject);
            helper.setText(body);
            helper.setTo(mailUsername);
            // We set BCC instead of TO. This is crucial for privacy!
            helper.setBcc(bccEmails.toArray(new String[0]));
            helper.setFrom(mailUsername); // Optional, usually inferred from properties

            javaMailSender.send(message);
            log.info("Sent broadcast to {} attendees", bccEmails.size());
        } catch (Exception e) {
            log.error("Failed to send broadcast email", e);
        }
    }
    public void sendEmailWithAttachment(String to, String subject, String body, String filename, byte[] bytes) {
        try {

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(filename,new ByteArrayResource(bytes));
            helper.setFrom(mailUsername); // Optional, usually inferred from properties

            javaMailSender.send(message);
            log.info("Email successfully sent to {}", to);
        } catch (Exception e) {
            log.error("Exception while sending email to {}", to, e);
        }
    }
}
