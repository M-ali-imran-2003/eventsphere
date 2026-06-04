package com.example.eventsphere.service;

import com.example.eventsphere.dto.AllTicketsDTO;
import com.example.eventsphere.dto.MyTicketResponse;
import com.example.eventsphere.dto.TicketTransferRequest;
import com.example.eventsphere.entity.*;
import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.OrgRole;
import com.example.eventsphere.enums.UserRole;
import com.example.eventsphere.enums.UserStatus;
import com.example.eventsphere.repository.*;
import com.example.eventsphere.utils.SecurityUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class TicketService {

    private final EmailService emailService;
    private final SpringTemplateEngine templateEngine;
    private final AttendeeTicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final TicketTierRepository tierRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TicketService(EmailService emailService, SpringTemplateEngine templateEngine, AttendeeTicketRepository ticketRepository, EventRepository eventRepository, OrderRepository orderRepository, TicketTierRepository tierRepository, OrganizationRepository organizationRepository, OrganizationMemberRepository organizationMemberRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.emailService = emailService;
        this.templateEngine = templateEngine;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
        this.tierRepository = tierRepository;
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    public List<MyTicketResponse> getMyTickets() {

        User user = userRepository.findByUsername(Objects.requireNonNull(SecurityUtil.getCurrentUser()).getUsername()).orElseThrow(()-> new RuntimeException("User Not Found"));

        // 1. Fetch raw tickets assigned to this email
        List<AttendeeTicket> rawTickets = ticketRepository.findByAssignedEmail(user.getEmail());

        List<MyTicketResponse> responseList = new ArrayList<>();

        // 2. Map them to the frontend DTO
        for (AttendeeTicket ticket : rawTickets) {
            // Fetch associated data. (In a highly optimized production app,
            // you might use a custom @Query with JOINs to do this in one database hit,
            // but for this phase, direct lookups are perfectly fine and clean).
            Order order = orderRepository.findById(ticket.getOrderId()).orElse(null);
            if (order == null) continue;

            Event event = eventRepository.findById(order.getEventId()).orElse(null);
            TicketTier tier = tierRepository.findById(ticket.getTierId()).orElse(null);

            if (event != null && tier != null) {
                responseList.add(MyTicketResponse.builder()
                        .ticketId(ticket.getId())
                        .ticketReference(ticket.getTicketReference())
                        .orderReference(order.getOrderReference())
                        .eventName(event.getTitle())
                        .eventDate(event.getStartDateTime())
                        .venue(event.getVenue())
                        .tierName(tier.getTierName())
                        .assignedName(ticket.getAssignedName())
                        .qrCodeHash(ticket.getQrCodeHash())
                        .isCheckedIn(ticket.isCheckedIn())
                        .build());
            }
        }

        // Sorts the final list so upcoming events appear first
        return responseList.stream()
                .sorted((t1, t2) -> t1.getEventDate().compareTo(t2.getEventDate()))
                .toList();
    }

    public List<AllTicketsDTO> getAllTicketsByEvent(UUID eventId) {
        User currentUser = SecurityUtil.getCurrentUser();

        // Verify ownership
        verifyEventOwnership(eventId, currentUser);

        // 1. Fetch all matching tickets in one database call
        List<AttendeeTicket> tickets = ticketRepository.findAllTicketsByEventId(eventId);

        // Optional check: if empty, stop immediately
        if (tickets.isEmpty()) {
            return List.of();
        }

        // 2. Fetch shared resources once to avoid N+1 lookups
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // 3. Map and sort cleanly using Java Streams
        return tickets.stream()
                .map(ticket -> {
                    // Fetch context-specific details safely
                    Order order = orderRepository.findById(ticket.getOrderId()).orElse(null);
                    TicketTier tier = tierRepository.findById(ticket.getTierId()).orElse(null);

                    if (order == null || tier == null) {
                        return null; // Skip corrupted or partial records safely
                    }

                    return AllTicketsDTO.builder()
                            .ticketId(ticket.getId())
                            .ticketReference(ticket.getTicketReference())
                            .orderReference(order.getOrderReference())
                            .eventName(event.getTitle())
                            .eventDate(event.getStartDateTime())
                            .tierName(tier.getTierName())
                            .assignedPhone(ticket.getAssignedPhone())
                            .assignedCnic(ticket.getAssignedCnic())
                            .assignedEmail(ticket.getAssignedEmail())
                            .assignedName(ticket.getAssignedName())
                            .isCheckedIn(ticket.isCheckedIn())
                            .checkedInTime(ticket.getCheckInTime())
                            .build();
                })
                .filter(Objects::nonNull) // Discard any skipped records
                .sorted(Comparator.comparing(AllTicketsDTO::getEventDate)) // Cleaner sorting syntax
                .toList();
    }

    @Transactional
    public String transferTicket(UUID ticketId, TicketTransferRequest request) {

        User user = userRepository.findByUsername(Objects.requireNonNull(SecurityUtil.getCurrentUser()).getUsername()).orElseThrow(()-> new RuntimeException("User Not Found"));

        AttendeeTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (!ticket.getAssignedEmail().equalsIgnoreCase(user.getEmail())) {
            throw new RuntimeException("You are not authorized to transfer this ticket.");
        }

        if (ticket.isCheckedIn()) {
            throw new RuntimeException("This ticket has already been scanned at the gate.");
        }

        // ==========================================
        // NEW: SILENT REGISTRATION FOR THE FRIEND
        // ==========================================
        boolean isNewUser = false;
        String rawTempPassword = null;

        User friend = userRepository.findByEmail(request.getNewEmail()).orElse(null);

        if (friend == null) {
            log.info("Friend email not found. Creating silent Attendee account for transfer.");
            isNewUser = true;
            friend = new User();

            friend.setEmail(request.getNewEmail());
            friend.setCnic(request.getNewCnic());
            friend.setPhoneNo(request.getNewPhone());
            friend.setName(request.getNewName());
            String emailPrefix = request.getNewEmail().split("@")[0];
            String uniqueSuffix = UUID.randomUUID().toString().substring(0, 4);
            friend.setUsername(emailPrefix + "_" + uniqueSuffix);
            friend.setStatus(UserStatus.ACTIVE);
            friend.setCreatedAt(LocalDateTime.now());
            friend.setModifiedAt(LocalDateTime.now());
            friend.setRole(UserRole.ATTENDEE); // Or however your roles are defined
            // A real implementation would generate a random password here
            rawTempPassword = SecurityUtil.generateTempPassword();
            friend.setPassword(passwordEncoder.encode(rawTempPassword));
            userRepository.save(friend);
        }

        // Update the ticket
        ticket.setAssignedName(request.getNewName());
        ticket.setAssignedEmail(request.getNewEmail());
        ticketRepository.save(ticket);

        // ==========================================
        // NEW: TRIGGER BACKGROUND TRANSFER EMAILS
        // ==========================================
        // We will call a new method in your TicketFulfillmentService
        //ticket.processTicketTransferEmails(ticket, user.getEmail(), friend, isNewUser, rawTempPassword);

        return "Ticket successfully transferred to " + request.getNewName();
    }

    private void verifyEventOwnership(UUID eventId, User user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found."));

        Organization org = organizationRepository.findById(event.getOrganizationId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        if(!org.getStatus().equals(AppStatus.ACTIVE)) {
            throw new RuntimeException("Organization is not active");
        }

        OrganizationMember member = organizationMemberRepository
                .findByOrganizationIdAndUserId(org.getId(), user.getId())
                .orElseThrow(() -> new RuntimeException("Access denied."));


        if (member.getRole() != OrgRole.OWNER) {
            throw new RuntimeException("Only Organization Owners can manage.");
        }
    }
}