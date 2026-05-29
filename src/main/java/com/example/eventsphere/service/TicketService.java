package com.example.eventsphere.service;

import com.example.eventsphere.entity.AttendeeTicket;
import com.example.eventsphere.entity.Event;
import com.example.eventsphere.entity.Order;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.repository.AttendeeTicketRepository;
import com.example.eventsphere.repository.EventRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
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
    public void generateAndSendTickets(Order order, User buyer) {
        log.info("Background Thread started: Generating tickets for Order: {}", order.getId());

        try {
            // 0. Fetch the extra data needed for the ticket design
            Event event = eventRepository.findById(order.getEventId())
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            List<AttendeeTicket> tickets = ticketRepository.findByOrderId(order.getId())
                    .orElseThrow(() -> new RuntimeException("Tickets not found"));

            // ========================================================
            // STEP 1: RENDER THE HTML TEMPLATE (Thymeleaf)
            // ========================================================
            Context context = new Context();
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

            log.info("Ticket email successfully dispatched to {} with {} tickets attached.",
                    buyer.getEmail(), tickets.size());

        } catch (Exception e) {
            log.error("Failed to deliver ticket emails for order {}", order.getId(), e);
        }
    }
}