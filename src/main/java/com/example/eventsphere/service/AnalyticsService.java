package com.example.eventsphere.service;

import com.example.eventsphere.dto.EventAnalyticsResponse;
import com.example.eventsphere.entity.*;
import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.OrgRole;
import com.example.eventsphere.enums.PaymentStatus;
import com.example.eventsphere.mapper.GenericMapper;
import com.example.eventsphere.repository.*;
import com.example.eventsphere.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final TicketTierRepository tierRepository;
    private final AttendeeTicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final GenericMapper mapper;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationRepository organizationRepository;
    private final FileService fileService;
    private final CategoryRepository categoryRepository;
    private final SubEventRepository subEventRepository;
    private final TicketTierRepository ticketTierRepository;
    private final LandingPageRepository landingPageRepository;

    public AnalyticsService(OrderRepository orderRepository, TicketTierRepository tierRepository, AttendeeTicketRepository ticketRepository, EventRepository eventRepository, GenericMapper mapper, OrganizationMemberRepository organizationMemberRepository, OrganizationRepository organizationRepository, FileService fileService, CategoryRepository categoryRepository, SubEventRepository subEventRepository, TicketTierRepository ticketTierRepository, LandingPageRepository landingPageRepository) {
        this.orderRepository = orderRepository;
        this.tierRepository = tierRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.mapper = mapper;
        this.organizationMemberRepository = organizationMemberRepository;
        this.organizationRepository = organizationRepository;
        this.fileService = fileService;
        this.categoryRepository = categoryRepository;
        this.subEventRepository = subEventRepository;
        this.ticketTierRepository = ticketTierRepository;
        this.landingPageRepository = landingPageRepository;
    }

    public EventAnalyticsResponse getEventAnalytics(UUID eventId) {
        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("Unauthorized. Please log in.");
        }
       verifyEventOwnership(eventId, currentUser);

            // 1. Calculate Capacity & Total Sold from Tiers
        List<TicketTier> tiers = tierRepository.findByEventId(eventId);
        int totalCapacity = tiers.stream().mapToInt(TicketTier::getTotalCapacity).sum();
        int totalTicketsSold = tiers.stream().mapToInt(TicketTier::getQuantitySold).sum();

        // 2. Calculate Revenue & Gate Scans from Orders
        List<Order> eventOrders = orderRepository.findByEventId(eventId);
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalCheckedIn = 0;

        for (Order order : eventOrders) {
            // Only count revenue for successful payments
            if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
                totalRevenue = totalRevenue.add(order.getTotalAmount());
            }

            // Count how many tickets for this order were scanned at the gate
            Optional<List<AttendeeTicket>> tickets = ticketRepository.findByOrderId(order.getId());
            totalCheckedIn += (int) tickets.orElse(List.of())
                    .stream()
                    .filter(AttendeeTicket::isCheckedIn)
                    .count();
        }

        int totalPending = totalTicketsSold - totalCheckedIn;

        // 3. Build and return the DTO
        return EventAnalyticsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalTicketsSold(totalTicketsSold)
                .totalCapacity(totalCapacity)
                .totalCheckedIn(totalCheckedIn)
                .totalPendingEntry(totalPending)
                .build();
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

    }
}