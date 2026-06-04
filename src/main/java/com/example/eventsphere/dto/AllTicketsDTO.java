package com.example.eventsphere.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AllTicketsDTO {
    private UUID ticketId;
    private String ticketReference;
    private String orderReference;
    private String eventName;
    private LocalDateTime eventDate;
    private String tierName; // e.g., "General Admission"
    private String assignedName;
    private String assignedCnic;
    private String assignedEmail;
    private String assignedPhone;
    private boolean isCheckedIn;
    private LocalDateTime checkedInTime;
}