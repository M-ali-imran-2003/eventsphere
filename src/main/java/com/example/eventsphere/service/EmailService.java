package com.example.eventsphere.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            // message.setFrom("your.email@gmail.com"); // Optional, usually inferred from properties

            javaMailSender.send(message);
            log.info("Email successfully sent to {}", to);
        } catch (Exception e) {
            log.error("Exception while sending email to {}", to, e);
        }
    }
}
