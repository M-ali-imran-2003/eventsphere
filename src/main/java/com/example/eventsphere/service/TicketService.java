package com.example.eventsphere.service;

import com.example.eventsphere.entity.Order;
import com.example.eventsphere.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TicketService {

    // Inject your EmailSender or TemplateEngine later when we configure SMTP
    private final EmailService emailService;

    public TicketService(EmailService emailService) {
        this.emailService = emailService;
    }
    // private final TemplateEngine templateEngine;

    /**
     * This method runs completely in the background.
     * The checkout API will already have returned a success message to Arsam's frontend
     * while this method is processing the heavy lifting.
     */
    @Async
    public void generateAndSendTickets(Order order, User buyer) {
        log.info("Background Thread started: Generating tickets for Order Reference: {}", order.getOrderReference());

        try {
            // ========================================================
            // STEP 1: RENDER THE HTML TEMPLATE (Thymeleaf)
            // ========================================================
            // We will pass the order, tickets, and event details to a Thymeleaf context.
            // String htmlContent = templateEngine.process("templates/ticket-email", context);

            // ========================================================
            // STEP 2: CONVERT HTML TO PRINT-READY PDF
            // ========================================================
            // Using a lightweight library like OpenPDF or FlyingSaucer,
            // we turn that exact HTML layout into a binary PDF byte array.
            // byte[] pdfBytes = pdfGenerator.generate(htmlContent);

            // ========================================================
            // STEP 3: SEND THE EMAIL WITH THE ATTACHMENT
            // ========================================================
            // Send the email to buyer.getEmail() with the PDF attached.

            // Simulating network delay of rendering and mailing (2 seconds)
            Thread.sleep(2000);

            log.info("Notification successfully dispatched to {}", buyer.getEmail());

        } catch (Exception e) {
            // CRITICAL: Because this is a background thread, errors here will NOT roll back
            // the main checkout database transaction. The user keeps their tickets,
            // and we log the error here so support can manually resend it if needed.
            log.error("Failed to deliver ticket emails for order {}", order.getId(), e);
        }
    }
}