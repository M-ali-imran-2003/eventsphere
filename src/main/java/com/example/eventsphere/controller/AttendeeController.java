package com.example.eventsphere.controller;

import com.example.eventsphere.dto.MyEventsResponse;
import com.example.eventsphere.dto.MyOrdersResponse;
import com.example.eventsphere.dto.MyTicketResponse;
import com.example.eventsphere.service.EventService;
import com.example.eventsphere.service.OrderService;
import com.example.eventsphere.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/attendee")
@RequiredArgsConstructor
public class AttendeeController {

    private final TicketService ticketService;
    private final OrderService orderService;
    private final EventService eventService;


    /**
     * Arsam will hit this endpoint when the dashboard loads.
     * The Principal object automatically contains the logged-in user's email
     * (provided by your Spring Security / JWT configuration).
     */
    @PreAuthorize("hasRole('ATTENDEE')")
    @GetMapping("/my-tickets")
    public ResponseEntity<List<MyTicketResponse>> getMyTickets() {

        List<MyTicketResponse> tickets = ticketService.getMyTickets();
        return ResponseEntity.ok(tickets);
    }

    @PreAuthorize("hasRole('ATTENDEE')")
    @GetMapping("/my-orders")
    public ResponseEntity<List<MyOrdersResponse>> getMyOrders() {

        List<MyOrdersResponse> orders = orderService.getMyOrders();
        return ResponseEntity.ok(orders);
    }

    @PreAuthorize("hasRole('ATTENDEE')")
    @GetMapping("/my-events")
    public ResponseEntity<List<MyEventsResponse>> getMyEvents() {

        List<MyEventsResponse> events = eventService.getMyEvents();
        return ResponseEntity.ok(events);
    }
}