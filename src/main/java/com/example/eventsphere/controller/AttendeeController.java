package com.example.eventsphere.controller;

import com.example.eventsphere.dto.MyTicketResponse;
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
}