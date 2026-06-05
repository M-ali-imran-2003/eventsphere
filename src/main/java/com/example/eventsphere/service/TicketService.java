package com.example.eventsphere.service;

import com.example.eventsphere.dto.AllTicketsDTO;
import com.example.eventsphere.dto.LostTicketRecoveryRequest;
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
import org.springframework.context.annotation.Lazy;
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
import java.util.stream.Collectors;

@Slf4j
@Service
public class TicketService {

    private final EmailService emailService;
    private final TicketService self; // ADD THIS
    private final SpringTemplateEngine templateEngine;
    private final AttendeeTicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final SponsorRepository sponsorRepository;
    private final TicketTierRepository tierRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TicketService(EmailService emailService, @Lazy TicketService self, SpringTemplateEngine templateEngine, AttendeeTicketRepository ticketRepository, EventRepository eventRepository, OrderRepository orderRepository, SponsorRepository sponsorRepository, TicketTierRepository tierRepository, OrganizationRepository organizationRepository, OrganizationMemberRepository organizationMemberRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.emailService = emailService;
        this.self = self;
        this.templateEngine = templateEngine;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.orderRepository = orderRepository;
        this.sponsorRepository = sponsorRepository;
        this.tierRepository = tierRepository;
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Async
    public void generateAndSendTickets(Order order, User buyer, boolean isNewUser, String tempPassword) {
        log.info("Generating tickets for Checkout Order: {}", order.getId());
        try {
            Event event = eventRepository.findById(order.getEventId()).orElseThrow();
            List<AttendeeTicket> tickets = ticketRepository.findByOrderId(order.getId()).orElseThrow();

            String greeting = "Hi " + buyer.getName() + ", your payment was successful!";
            byte[] pdfBytes = createTicketPdfBytes(order, event, tickets, greeting);

            String subject = "Your Tickets for " + event.getTitle();
            String body = "Thank you for your purchase! Your tickets are attached.";
            emailService.sendEmailWithAttachment(buyer.getEmail(), subject, body, "Tickets_" + order.getOrderReference() + ".pdf", pdfBytes);

            if (isNewUser && tempPassword != null) {
                sendWelcomeEmail(buyer.getEmail(), buyer.getName(),tempPassword,buyer.getUsername() );
            }
        } catch (Exception e) {
            log.error("Checkout email failed", e);
        }
    }

    @Async
    public void resendTicketsAsync(List<AttendeeTicket> tickets, Event event, Order order, String email) {
        log.info("Generating Recovery PDF for {} tickets...", tickets.size());
        try {
            String greeting = "Hi " + tickets.get(0).getAssignedName() + ", here are your recovered tickets.";

            // Generate ONE PDF containing ALL their tickets!
            byte[] pdfBytes = createTicketPdfBytes(order, event, tickets, greeting);

            String subject = "Recovered Tickets: " + event.getTitle();
            String body = "As requested, please find your recovered tickets attached.";
            emailService.sendEmailWithAttachment(email, subject, body, "Recovered_Tickets.pdf", pdfBytes);

        } catch (Exception e) {
            log.error("Recovery email failed", e);
        }
    }

    @Async
    public void sendTransferEmailsAsync(AttendeeTicket ticket, String oldEmail) {
        log.info("Processing Transfer Emails for Ticket: {}", ticket.getTicketReference());
        try {
            Order order = orderRepository.findById(ticket.getOrderId()).orElseThrow();
            Event event = eventRepository.findById(order.getEventId()).orElseThrow();

            // 1. Send PDF to the NEW owner
            String greeting = "Hi " + ticket.getAssignedName() + ", you've been transferred a ticket!";
            byte[] pdfBytes = createTicketPdfBytes(order, event, java.util.Collections.singletonList(ticket), greeting);

            String newSubject = "You received a ticket to " + event.getTitle() + "!";
            String newBody = "Great news! Someone transferred a ticket to you. See attached.";
            emailService.sendEmailWithAttachment(ticket.getAssignedEmail(), newSubject, newBody, "Transferred_Ticket-"+ticket.getTicketReference()+".pdf", pdfBytes);

            // 2. Send plain text confirmation to the OLD owner
            if (!oldEmail.equalsIgnoreCase(ticket.getAssignedEmail())) {
                String oldSubject = "Transfer Successful: " + ticket.getTicketReference();
                String oldBody = "Your ticket has been successfully transferred to " + ticket.getAssignedEmail() + ".";
                emailService.sendEmail(oldEmail, oldSubject, oldBody);
            }
        } catch (Exception e) {
            log.error("Transfer emails failed", e);
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
        User user = userRepository.findByUsername(Objects.requireNonNull(SecurityUtil.getCurrentUser()).getUsername())
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        // 1. Fetch raw tickets
        List<AttendeeTicket> rawTickets = ticketRepository.findByAssignedEmail(user.getEmail());
        if (rawTickets.isEmpty()) return List.of();

        // 2. Extract unique Order and Tier IDs
        Set<UUID> orderIds = rawTickets.stream().map(AttendeeTicket::getOrderId).collect(Collectors.toSet());
        Set<UUID> tierIds = rawTickets.stream().map(AttendeeTicket::getTierId).collect(Collectors.toSet());

        // 3. Bulk fetch Orders and Tiers
        Map<UUID, Order> orderMap = orderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, order -> order));
        Map<UUID, TicketTier> tierMap = tierRepository.findAllById(tierIds).stream()
                .collect(Collectors.toMap(TicketTier::getId, tier -> tier));

        // 4. Extract Event IDs from the fetched Orders and bulk fetch Events
        Set<UUID> eventIds = orderMap.values().stream().map(Order::getEventId).collect(Collectors.toSet());
        Map<UUID, Event> eventMap = eventRepository.findAllById(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, event -> event));

        // 5. Map to DTO in memory (No database calls inside the loop)
        return rawTickets.stream()
                .map(ticket -> {
                    Order order = orderMap.get(ticket.getOrderId());
                    if (order == null) return null;

                    Event event = eventMap.get(order.getEventId());
                    TicketTier tier = tierMap.get(ticket.getTierId());

                    if (event != null && tier != null) {
                        return MyTicketResponse.builder()
                                .ticketId(ticket.getId())
                                .ticketReference(ticket.getTicketReference())
                                .orderReference(order.getOrderReference())
                                .eventName(event.getTitle())
                                .eventDate(event.getStartDateTime())
                                .venue(event.getVenue())
                                .tierName(tier.getTierName())
                                .assignedName(ticket.getAssignedName())
                                .assignedCnic(ticket.getAssignedCnic())
                                .assignedPhone(ticket.getAssignedPhone())
                                .assignedEmail(ticket.getAssignedEmail())
                                .qrCodeHash(ticket.getQrCodeHash())
                                .isCheckedIn(ticket.isCheckedIn())
                                .build();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(MyTicketResponse::getEventDate))
                .toList();
    }

    public List<AllTicketsDTO> getAllTicketsByEvent(UUID eventId) {
        User currentUser = SecurityUtil.getCurrentUser();
        verifyEventOwnership(eventId, currentUser);

        // 1. Fetch all tickets
        List<AttendeeTicket> tickets = ticketRepository.findAllTicketsByEventId(eventId);
        if (tickets.isEmpty()) return List.of();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // 2. Extract unique Order IDs and Tier IDs
        Set<UUID> orderIds = tickets.stream().map(AttendeeTicket::getOrderId).collect(Collectors.toSet());
        Set<UUID> tierIds = tickets.stream().map(AttendeeTicket::getTierId).collect(Collectors.toSet());

        // 3. Bulk fetch Orders and Tiers
        Map<UUID, Order> orderMap = orderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, order -> order));
        Map<UUID, TicketTier> tierMap = tierRepository.findAllById(tierIds).stream()
                .collect(Collectors.toMap(TicketTier::getId, tier -> tier));

        // 4. Map the DTOs in memory
        return tickets.stream()
                .map(ticket -> {
                    Order order = orderMap.get(ticket.getOrderId());
                    TicketTier tier = tierMap.get(ticket.getTierId());

                    if (order == null || tier == null) return null;

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
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(AllTicketsDTO::getEventDate))
                .toList();
    }

    @Transactional
    public String transferTicket(UUID ticketId, TicketTransferRequest request) {

        User currentUser = SecurityUtil.getCurrentUser();

        AttendeeTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // SECURITY: Verify the person requesting the transfer actually owns the order!
        Order order = orderRepository.findById(ticket.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getBuyerId().equals(currentUser.getId())) {
            throw new RuntimeException("You do not have permission to transfer this ticket.");
        }

        // THE ANTI-SCALPING CHECK (1-Time Transfer Limit)
        if (ticket.isTransferred()) {
            throw new RuntimeException("This ticket has already been transferred once and cannot be transferred again.");
        }

        // Log the old email so we can send them a confirmation
        String oldEmail = ticket.getAssignedEmail();

        // Update the ticket to the new person
        ticket.setAssignedName(request.getNewName());
        ticket.setAssignedCnic(request.getNewCnic());
        ticket.setAssignedEmail(request.getNewEmail());
        ticket.setAssignedPhone(request.getNewPhone());
        ticket.setTransferred(true); // Lock it forever!

        ticketRepository.saveAndFlush(ticket);

        // TRIGGER THE EMAILS (This should be your @Async email method)
        // 1. Email the old owner saying "Your ticket was transferred"
        // 2. Email the PDF to the newAssignedEmail
        self.sendTransferEmailsAsync(ticket, oldEmail);

        return "Ticket successfully transferred to " + request.getNewEmail();
    }

    public String recoverLostTickets(UUID eventId, LostTicketRecoveryRequest request) {
        List<AttendeeTicket> tickets = ticketRepository.findByEventIdAndAssignedEmail(eventId, request.getEmail());

        if (!tickets.isEmpty()) {
            Event event = eventRepository.findById(eventId).orElseThrow();

            // 1. Group the tickets by their exact Order ID
            Map<UUID, List<AttendeeTicket>> ticketsByOrder = tickets.stream()
                    .collect(Collectors.groupingBy(AttendeeTicket::getOrderId));

            // 2. Loop through each separate order
            for (Map.Entry<UUID, List<AttendeeTicket>> entry : ticketsByOrder.entrySet()) {
                UUID orderId = entry.getKey();
                List<AttendeeTicket> orderTickets = entry.getValue();

                // Fetch the exact order for this specific group of tickets
                Order order = orderRepository.findById(orderId).orElseThrow();

                // 3. Fire the background thread for THIS specific order
                // (Using 'self.' so the @Async proxy works properly)
                self.resendTicketsAsync(orderTickets, event, order, request.getEmail());
            }
        }

        // Always return the generic message for security
        return "If tickets exist for this email, they have been sent to your inbox.";
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

    private byte[] createTicketPdfBytes(Order order, Event event, List<AttendeeTicket> tickets, String greetingMessage) {
        // 1. Fetch Sponsors and Organizer Name
        List<Sponsor> sponsors = sponsorRepository.findByEventId(event.getId());
        // Fetch Organization name (Adjust this based on how you store Organizers!)
        String orgName = "Unknown Organizer";
        var orgOpt = organizationRepository.findById(event.getOrganizationId());
        if(orgOpt.isPresent()){
            orgName = orgOpt.get().getName();
        }

        // NEW: Fetch Tier Names and put them in a Map!
        List<TicketTier> tiers = tierRepository.findByEventId(event.getId());
        Map<UUID, String> tierNames = new java.util.HashMap<>();
        for (TicketTier tier : tiers) {
            tierNames.put(tier.getId(), tier.getTierName());
        }

        // 2. Generate QR Codes
        for (AttendeeTicket ticket : tickets) {
            try {
                String qrData = ticket.getQrCodeHash();
                BitMatrix bitMatrix = new MultiFormatWriter().encode(qrData, BarcodeFormat.QR_CODE, 200, 200);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
                String base64Qr = "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
                ticket.setQrCodeHash(base64Qr); // Temporarily set for Thymeleaf
            } catch (Exception e) {
                log.error("QR Code failed for {}", ticket.getTicketReference());
            }
        }

        // 3. Render HTML
        Context context = new Context();
        try {
            String logoPath = new ClassPathResource("static/images/logo.png").getURI().toString();
            context.setVariable("logoPath", logoPath);
        } catch (Exception e) {
            context.setVariable("logoPath", "");
        }

        context.setVariable("greetingMessage", greetingMessage);
        context.setVariable("order", order);
        context.setVariable("event", event);
        context.setVariable("tickets", tickets);
        context.setVariable("sponsors", sponsors);
        context.setVariable("organizationName", orgName);
        context.setVariable("tierNames", tierNames); // PASS THE TIER NAMES TO THYMELEAF!

        String htmlContent = templateEngine.process("ticket-template", context);

        // 4. Convert to PDF
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}