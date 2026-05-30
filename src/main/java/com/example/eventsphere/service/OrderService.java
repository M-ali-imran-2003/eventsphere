package com.example.eventsphere.service;

import com.example.eventsphere.dto.OrderSummaryResponse;
import com.example.eventsphere.entity.*;
import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.mapper.GenericMapper;
import com.example.eventsphere.repository.*;
import com.example.eventsphere.utils.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final UserRepository userRepository;
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

    @Autowired
    public OrderService(UserRepository userRepository, OrderRepository orderRepository, TicketTierRepository tierRepository, AttendeeTicketRepository ticketRepository, EventRepository eventRepository, GenericMapper mapper, OrganizationMemberRepository organizationMemberRepository, OrganizationRepository organizationRepository, FileService fileService, CategoryRepository categoryRepository, SubEventRepository subEventRepository, TicketTierRepository ticketTierRepository, LandingPageRepository landingPageRepository) {
        this.userRepository = userRepository;
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

    public List<OrderSummaryResponse> getEventOrders(UUID eventId) {

        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("Unauthorized. Please log in.");
        }
        verifyEventOwnership(eventId, currentUser);
        // Fetch all orders for this event
        List<Order> eventOrders = orderRepository.findByEventId(eventId);

        List<OrderSummaryResponse> responseList = new ArrayList<>();

        for (Order order : eventOrders) {
            // Fetch the buyer details (Silent Registration accounts)
            String buyerName = "Unknown";
            String buyerEmail = "Unknown";
            String buyerCnic = "Unknown";
            String buyerPhone = "Unknown";

            // Assuming your Order entity has a getBuyerId()
            var buyerOpt = userRepository.findById(order.getBuyerId());
            if (buyerOpt.isPresent()) {
                buyerName = buyerOpt.get().getName();
                buyerEmail = buyerOpt.get().getEmail();
                buyerCnic = buyerOpt.get().getCnic();
                buyerPhone = buyerOpt.get().getPhoneNo();

            }

            responseList.add(OrderSummaryResponse.builder()
                    .orderId(order.getId())
                    .orderReference(order.getTransactionReference()) // Or getOrderReference() if you added it
                    .buyerName(buyerName)
                    .buyerEmail(buyerEmail)
                    .buyerPhone(buyerPhone)
                    .buyerCnic(buyerCnic)
                    .totalAmount(order.getTotalAmount())
                    .paymentStatus(order.getPaymentStatus().name()) // Assuming it's an Enum
                    .createdAt(order.getCreatedAt())
                    .build());
        }

        // Sort dynamically: Newest orders at the top of the list
        return responseList.stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .toList();
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
