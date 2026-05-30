package com.example.eventsphere.service;

import com.example.eventsphere.entity.AttendeeTicket;
import com.example.eventsphere.entity.Event;
import com.example.eventsphere.entity.Order;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.repository.AttendeeTicketRepository;
import com.example.eventsphere.repository.EventRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class TicketService {

    private final EmailService emailService;
    private final SpringTemplateEngine templateEngine;
    private final AttendeeTicketRepository ticketRepository;
    private final EventRepository eventRepository;

    public TicketService(EmailService emailService, SpringTemplateEngine templateEngine, AttendeeTicketRepository ticketRepository, EventRepository eventRepository) {
        this.emailService = emailService;
        this.templateEngine = templateEngine;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
    }

    @Async
    public void generateAndSendTickets(Order order, User buyer,boolean isNewUser, String tempPassword) {
        log.info("Background Thread started: Generating tickets for Order: {}", order.getId());

        try {
            // 0. Fetch the extra data needed for the ticket design
            Event event = eventRepository.findById(order.getEventId())
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            List<AttendeeTicket> tickets = ticketRepository.findByOrderId(order.getId())
                    .orElseThrow(() -> new RuntimeException("Tickets not found"));

            for (AttendeeTicket ticket : tickets) {
                try {
                    // ENCODE ONLY THE RAW UUID HASH STRING
                    String qrData = ticket.getQrCodeHash();

                    BitMatrix bitMatrix = new MultiFormatWriter().encode(qrData, BarcodeFormat.QR_CODE, 200, 200);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);

                    String base64Qr = "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());

                    // Temporarily change the hash string into the Base64 image data for Thymeleaf to print
                    ticket.setQrCodeHash(base64Qr);
                } catch (Exception qrEx) {
                    log.error("Failed to generate QR code image for ticket reference: {}", ticket.getTicketReference(), qrEx);
                }
            }

            // ========================================================
            // STEP 1: RENDER THE HTML TEMPLATE (Thymeleaf)
            // ========================================================
            Context context = new Context();
            try {
                String logoPath = new ClassPathResource("static/images/logo.png").getURI().toString();
                context.setVariable("logoPath", logoPath); // Pass string starting with file:/...
            } catch (Exception e) {
                context.setVariable("logoPath", "");
            }
            context.setVariable("buyer", buyer);
            context.setVariable("order", order);
            context.setVariable("event", event);
            context.setVariable("tickets", tickets);

            // This looks for a file named "ticket-template.html" in src/main/resources/templates/
            String htmlContent = templateEngine.process("ticket-template", context);

            // ========================================================
            // STEP 2: CONVERT HTML TO PRINT-READY PDF
            // ========================================================
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();

            // Load the HTML string into the renderer
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();

            // Create the PDF in memory (no need to save to hard drive!)
            renderer.createPDF(outputStream);
            byte[] pdfBytes = outputStream.toByteArray();

            // ========================================================
            // STEP 3: SEND THE EMAIL WITH THE ATTACHMENT
            // ========================================================

            String to = buyer.getEmail();
            String subject = "Your Tickets for " + event.getTitle(); // Assuming Event has getTitle()
            String body = "Hello " + buyer.getName() + ",\n\nThank you for your purchase! Your tickets for "
                    + event.getTitle() + " are attached to this email as a PDF.\n\nSee you there!";

            emailService.sendEmailWithAttachment(to,subject,body,"EventSphere_Tickets_" + order.getOrderReference() + ".pdf", pdfBytes);

            if (isNewUser && tempPassword != null) {
                sendWelcomeEmail(buyer.getEmail(), buyer.getName(), tempPassword, buyer.getUsername());
            }

            log.info("Ticket email successfully dispatched to {} with {} tickets attached.",
                    buyer.getEmail(), tickets.size());

        } catch (Exception e) {
            log.error("Failed to deliver ticket emails for order {}", order.getId(), e);
        }
    }
    private void sendWelcomeEmail(String to, String name, String tempPassword, String username) {
        try {
            String subject = "Welcome to EventSphere - Your Account Details";
            String body= "Hi " + name + ",\n\n" +
                    "Thank you for your purchase! An EventSphere account has been automatically created for you so you can manage your tickets.\n\n" +
                    "Login Username: " + username + "\n" +
                    "Temporary Password: " + tempPassword + "\n\n" +
                    "Please log in at your earliest convenience to change your password and view your tickets.\n\n" +
                    "Best regards,\nThe EventSphere Team";

            emailService.sendEmail(to,subject,body);
            log.info("Welcome email with temporary credentials sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}", to, e);
        }
    }
}