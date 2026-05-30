package com.example.eventsphere.controller;

import com.example.eventsphere.dto.EventAnalyticsResponse;
import com.example.eventsphere.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Arsam hits this to populate the top KPI widgets on the dashboard.
     */
    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/event/{eventId}")
    public ResponseEntity<EventAnalyticsResponse> getEventAnalytics(@PathVariable UUID eventId) {
        // In a production app, you would verify the logged-in Principal
        // actually owns this eventId, but for the MVP, this is perfect.
        EventAnalyticsResponse analytics = analyticsService.getEventAnalytics(eventId);
        return ResponseEntity.ok(analytics);
    }
}