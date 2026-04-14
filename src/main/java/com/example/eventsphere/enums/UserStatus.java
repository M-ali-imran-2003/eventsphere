package com.example.eventsphere.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum UserStatus {

    ACTIVE("Active"),
    INACTIVE("Inactive"),
    SUSPENDED("Suspended");

    private final String label;

    UserStatus(String label) { this.label = label; }

    // Helper to get everything the UI needs for a dropdown
    public static List<Map<String, String>> getDropdownValues() {
        return Arrays.stream(values())
                .map(status -> Map.of(
                        "value", status.name(),
                        "label", status.getLabel()
                ))
                .collect(Collectors.toList());
    }
}
