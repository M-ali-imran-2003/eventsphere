package com.example.eventsphere.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateEventRequest {

    // --- Core Details ---
    private String title;
    private UUID categoryId;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;

    // --- Details & Media ---
    private String description;
    private MultipartFile image;

    // --- Location Data (Populated via Frontend Map API) ---
    private String venue;
    private Double lat;
    private Double lon;
    private String address;
    private String city;
    private String state;
    private String country;

    // --- Metadata & Config ---
    private String layoutType; // e.g., "GENERAL_ADMISSION", "SEATED_MAP", "VIRTUAL_STREAM"
    private List<String> tags;
    private String typeSpecificData; // JSON string for category-specific frontend fields
}