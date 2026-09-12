package com.example.eventsphere.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScanTicketResponse {
    private String assignedName;
    private String tierName;
    private LocalDateTime checkInTime;
    private Double distanceMeters;   // useful for Arsam to show/debug
}