package com.example.eventsphere.dto;

import com.example.eventsphere.enums.EventStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@Data
public class MyEventsResponse {
    private String title;
    private String organization;
    private String category;
    private String image;
    private String slug;
    private LocalDateTime start;
    private String venue;
    private String address;
    private String city;
    private String state;
    private String country;
}
