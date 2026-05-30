package com.example.eventsphere.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EventAnalyticsResponse {

    // --- Money & Sales ---
    private BigDecimal totalRevenue;
    private int totalTicketsSold;
    private int totalCapacity;      // Sum of all tier capacities

    // --- Gate Operations ---
    private int totalCheckedIn;     // How many people have arrived?
    private int totalPendingEntry;  // How many are we still waiting for?

    // Optional: You could add a list of top-selling tiers here later,
    // but this covers the core MVP metrics perfectly.
}