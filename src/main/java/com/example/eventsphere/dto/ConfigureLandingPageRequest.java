package com.example.eventsphere.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ConfigureLandingPageRequest {

    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug can only contain lowercase letters, numbers, and hyphens (e.g., tech-summit-2026)")
    private String slug;

    private String themeConfigJson;
}
