package com.example.eventsphere.dto;

import com.example.eventsphere.enums.SponsorTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SponsorRequest {

    @NotBlank(message = "Sponsor name is required")
    private String name;

    @NotNull(message = "Sponsor tier is required (e.g., PLATINUM, GOLD)")
    private SponsorTier sponsorTier;

    // These can be optional depending on your rules
    @NotNull(message = "Sponsor logo is required")
    private MultipartFile logo;

    private String websiteUrl;
}