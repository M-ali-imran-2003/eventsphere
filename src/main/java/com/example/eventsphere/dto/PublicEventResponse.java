package com.example.eventsphere.dto;

import com.example.eventsphere.entity.Event;
import com.example.eventsphere.entity.TicketTier;
import com.example.eventsphere.entity.SubEvent;
import com.example.eventsphere.enums.SponsorTier;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PublicEventResponse {
    private EventCoreDetails eventDetails;
    private LandingPageDesign landingPageDesign;
    private List<TicketTierDetails> tickets;
    private List<SubEventDetails> agenda;
    private List<SponsorDetails> sponsors;

    @Data
    @Builder
    public static class EventCoreDetails {
        private UUID id;
        private String title;
        private String description;
        private String imageUrl;
        private LocalDateTime startDatetime;
        private LocalDateTime endDatetime;
        private String venueName;
        private String formattedAddress;
        private List<String> searchTags;
    }

    @Data
    @Builder
    public static class LandingPageDesign {
        private String customSlug;
        private String themeConfigJson;
    }

    @Data
    @Builder
    public static class TicketTierDetails {
        private UUID id;
        private String tierName;
        private BigDecimal price;
        private boolean isSoldOut;
    }

    @Data
    @Builder
    public static class SubEventDetails {
        private UUID id;
        private String title;
        private String description;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String roomOrLocation;
        private String imageUrl; // Speaker headshot
    }

    @Data
    @Builder
    public static class SponsorDetails {
        private String name;
        private SponsorTier sponsorTier;
        private String logoUrl;
        private String websiteUrl;
    }
}