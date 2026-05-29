package com.example.eventsphere.dto;

import com.example.eventsphere.enums.EventStatus;
import com.example.eventsphere.utils.LocationUtil;
import lombok.Builder;
import lombok.Data;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PublicEventCardDTO {
    private String title;
    private String organization;
    private String category;
    private String slug; // CRITICAL: So the frontend knows where to link!// CRITICAL: So the frontend knows where to link!
    private String image;
    private LocalDateTime start;
    private String venue;
    private String city;
    private String state;
    private String country;
    private List<String> tags;

    public PublicEventCardDTO(  String title,
     String organization,
     String category,
     String slug, // CRITICAL: So the frontend knows where to link!// CRITICAL: So the frontend knows where to link!
     String image,
     LocalDateTime start,
     String venue,
     String city,
     String state,
     String country,
     List<String> tags) {
        this.title = title;
                this.organization = organization;
                this.category = category;
                this.slug = slug; // CRITICAL: So the frontend knows where to link!// CRITICAL: So the frontend knows where to link!
                this.image = image;
                this.start = start;
                this.venue = venue;
                this.city= city;
                this.state =state;
                this.country = country;
                this.tags = tags;
    }
}