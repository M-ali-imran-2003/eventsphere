package com.example.eventsphere.prediction;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/analytics/events")
@RequiredArgsConstructor
public class TierDemandForecastController {
    private final TierDemandForecastService service;

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/{eventId}/demand-forecast")
    public ResponseEntity<TierDemandDtos.ForecastResponse> get(@PathVariable UUID eventId) {
        return ResponseEntity.ok(service.forecast(eventId));
    }
}
