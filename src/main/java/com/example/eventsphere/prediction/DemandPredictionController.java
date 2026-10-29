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
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class DemandPredictionController {
    private final DemandPredictionService predictionService;

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<PredictionDtos.DashboardResponse> dashboard(
            @PathVariable UUID organizationId) {
        return ResponseEntity.ok(predictionService.getDashboard(organizationId));
    }
}
