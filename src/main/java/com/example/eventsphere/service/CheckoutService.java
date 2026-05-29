package com.example.eventsphere.service;

import com.example.eventsphere.dto.CheckoutRequest;
import com.example.eventsphere.dto.PaymentResult;
import com.example.eventsphere.entity.*;
import com.example.eventsphere.enums.PaymentStatus;
import com.example.eventsphere.enums.TransactionType;
import com.example.eventsphere.enums.UserRole;
import com.example.eventsphere.enums.UserStatus;
import com.example.eventsphere.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;

@Slf4j
@Service
public class CheckoutService {

    private final PaymentProcessor paymentProcessor;

    // Repositories mapped to your DDL tables
    private final OrderRepository orderRepository;
    private final TicketTierRepository tierRepository;
    private final AttendeeTicketRepository attendeeTicketRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final SubEventRegistrationRepository subEventRegRepository;
    private final WalletTransactionRepository walletRepository;
    private final TicketService ticketService;

    // We need these to fetch the Workspace ID and handle Silent Registration
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public CheckoutService(PaymentProcessor paymentProcessor, OrderRepository orderRepository, TicketTierRepository tierRepository, AttendeeTicketRepository attendeeTicketRepository, DiscountCodeRepository discountCodeRepository, SubEventRegistrationRepository subEventRegRepository, WalletTransactionRepository walletRepository, TicketService ticketService, EventRepository eventRepository, UserRepository userRepository) {
        this.paymentProcessor = paymentProcessor;
        this.orderRepository = orderRepository;
        this.tierRepository = tierRepository;
        this.attendeeTicketRepository = attendeeTicketRepository;
        this.discountCodeRepository = discountCodeRepository;
        this.subEventRegRepository = subEventRegRepository;
        this.walletRepository = walletRepository;
        this.ticketService = ticketService;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }
    // private final DiscountCodeRepository discountRepository; // Assuming you have this

    @Transactional
    public String processCheckout(UUID eventId, CheckoutRequest request) {
        log.info("Starting checkout for user {} on event {}", request.getBuyerEmail(), eventId);

        // Fetch the event to get the workspace_id later for the wallet
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // ==========================================
        // PHASE 0: SILENT REGISTRATION
        // ==========================================

        // Check if user exists. If not, create a placeholder ATTENDEE account
        User buyer = userRepository.findByEmail(request.getBuyerEmail()).orElseGet(() -> {
            log.info("New email detected. Creating silent Attendee account.");
            User newUser = new User();
            newUser.setEmail(request.getBuyerEmail());
            newUser.setCnic(request.getBuyerCnic());
            newUser.setPhoneNo(request.getBuyerPhone());
            newUser.setName(request.getBuyerName());
            String emailPrefix = request.getBuyerEmail().split("@")[0];
            String uniqueSuffix = UUID.randomUUID().toString().substring(0, 4);
            newUser.setUsername(emailPrefix + "_" + uniqueSuffix);
            newUser.setStatus(UserStatus.ACTIVE);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setModifiedAt(LocalDateTime.now());
            newUser.setRole(UserRole.ATTENDEE); // Or however your roles are defined
            // A real implementation would generate a random password here
            return userRepository.saveAndFlush(newUser);
        });

        // ==========================================
        // PHASE 1: PRE-CHECKS & MATH
        // ==========================================

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CheckoutRequest.TicketSelection selection : request.getTicketSelections()) {
            TicketTier tier = tierRepository.findById(selection.getTierId())
                    .orElseThrow(() -> new RuntimeException("Ticket Tier Not Found"));

            // Check capacity
            int availableTickets = tier.getTotalCapacity() - tier.getQuantitySold();
            if (availableTickets < selection.getQuantity()) {
                throw new RuntimeException("Not enough tickets available for tier: " + tier.getTierName());
            }

            // UPDATE DB: Lock in the quantity so no one else can buy them during this transaction
            tier.setQuantitySold(tier.getQuantitySold() + selection.getQuantity());
            tierRepository.saveAndFlush(tier);

            // Calculate cost
            BigDecimal selectionQuantity = BigDecimal.valueOf(selection.getQuantity());
            BigDecimal selectionCost = tier.getPrice().multiply(selectionQuantity);
            totalAmount = totalAmount.add(selectionCost);
        }

        // 2. Apply Discount Code (If provided)
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            DiscountCode discount = discountCodeRepository.findByCodeAndEventId(request.getPromoCode(), eventId)
                    .orElseThrow(() -> new RuntimeException("Invalid Promo Code"));

            // Check 1: Is it manually deactivated?
            if (!discount.isActive()) {
                throw new RuntimeException("This promo code is no longer active.");
            }

            // Check 2: Has it reached its usage limit?
            if (discount.getTimesUsed() >= discount.getMaxUses()) {
                throw new RuntimeException("This promo code has reached its usage limit.");
            }

            // Check 3: Has it expired? (Assuming you have a valid_until column)
            LocalDateTime now = LocalDateTime.now();
            if (discount.getValidUntil() != null && now.isAfter(discount.getValidUntil())) {
                throw new RuntimeException("This promo code has expired.");
            }

            log.info("Applying promo code: {}", request.getPromoCode());

            // Calculate the actual discount.
            // Assuming your table has a 'discount_percentage' column (e.g., 15 for 15% off)
            BigDecimal percentage = discount.getDiscountValue();

            // Formula: 1 - (percentage / 100). Example: 15% becomes 0.85 multiplier.
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(percentage.divide(new BigDecimal("100")));
            totalAmount = totalAmount.multiply(discountMultiplier);

            // UPDATE DB: Lock in the usage so people can't abuse it!
            // Because this is inside your @Transactional method, it will safely rollback if the payment fails.
            discount.setTimesUsed(discount.getTimesUsed() + 1);
            discountCodeRepository.saveAndFlush(discount);
        }

        // ==========================================
        // PHASE 2: TAKE THE MONEY
        // ==========================================

        PaymentResult payment = paymentProcessor.processPayment(totalAmount, null);

        if (!payment.isSuccessful()) {
            log.warn("Payment failed for {}. Reason: {}", request.getBuyerEmail(), payment.getErrorMessage());
            throw new RuntimeException("Checkout failed: " + payment.getErrorMessage());
        }

        // ==========================================
        // PHASE 3: FULFILLMENT (Database Writes)
        // ==========================================

        // 1. Create the Order
        Order order = new Order();
        order.setBuyerId(buyer.getId());
        order.setEventId(eventId);
        order.setTotalAmount(totalAmount);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setOrderReference(generateHumanReadableId("ORD"));
        order.setTransactionReference(payment.getTransactionId());
        // order.setGatewayId(...); // Set if you track which gateway was used
        order.setCreatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.saveAndFlush(order);

        // 2. Generate the Tickets
        String buyerName = request.getBuyerName();

        for (CheckoutRequest.TicketSelection selection : request.getTicketSelections()) {
            for (int i = 0; i < selection.getQuantity(); i++) {

                AttendeeTicket ticket = new AttendeeTicket();
                ticket.setOrderId(savedOrder.getId());
                ticket.setTierId(selection.getTierId());
                ticket.setTicketReference(generateHumanReadableId("TKT"));
                ticket.setAssignedName(buyerName); // Default assignment
                ticket.setAssignedEmail(buyer.getEmail());
                ticket.setQrCodeHash(UUID.randomUUID().toString()); // The magic scan code
                ticket.setCheckedIn(false);

                AttendeeTicket savedTicket = attendeeTicketRepository.saveAndFlush(ticket);

                // 3. Reserve Sub-Events (If any)
                if (request.getSelectedSubEventIds() != null && !request.getSelectedSubEventIds().isEmpty()) {
                    for (UUID subEventId : request.getSelectedSubEventIds()) {
                        SubEventRegistration subReg = new SubEventRegistration();
                        // Assuming your entity uses an embedded ID or just fields
                        subReg.setTicketId(savedTicket.getId());
                        subReg.setSubEventId(subEventId);
                        subEventRegRepository.saveAndFlush(subReg);
                    }
                }
            }
        }

        // 4. Update Organizer's Wallet
        WalletTransactions walletTx = new WalletTransactions();
        walletTx.setOrganizationId(event.getOrganizationId()); // Assuming Event has workspace_id
        walletTx.setAmount(totalAmount); // Give the money to the organizer
        walletTx.setTransactionType(TransactionType.CREDIT);
        walletTx.setDescription("Revenue from Order: " + savedOrder.getId());
        walletTx.setReferenceId(savedOrder.getId());
        walletTx.setStatus(PaymentStatus.SUCCESS);
        walletTx.setCreatedAt(LocalDateTime.now());

        walletRepository.saveAndFlush(walletTx);
        ticketService.generateAndSendTickets(savedOrder, buyer);

        log.info("Checkout successful! Order ID generated: {}", savedOrder.getId());

        return "Checkout complete! Transaction ID: " + payment.getTransactionId();
    }

    private String generateHumanReadableId(String prefix) {
        // Generates a string like "ORD-2026-A4F89Z"
        int year = LocalDateTime.now().getYear();
        String randomString = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + "-" + year + "-" + randomString;
    }
}