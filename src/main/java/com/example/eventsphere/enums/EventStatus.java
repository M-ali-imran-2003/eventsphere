package com.example.eventsphere.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum EventStatus {
    DRAFT("Draft"),
    PUBLISHED("Published"),
    CANCELLED("Cancelled"),
    INACTIVE("Inactive"),
    ACTIVE("Active");

    private final String label;

    EventStatus(String label) { this.label = label; }

    // Helper to get everything the UI needs for a dropdown
    public static List<Map<String, String>> getDropdownValues() {
        return Arrays.stream(values())
                .map(role -> Map.of(
                        "value", role.name(),  // "ADMIN"
                        "label", role.getLabel() // "Admin"
                ))
                .collect(Collectors.toList());
    }
}
