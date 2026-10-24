package com.example.eventsphere.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum ChatRole {

    // 1. Change these to lowercase to match the DB exactly ⬇️
    assistant("Assistant"),
    user("User");

    private final String label;

    ChatRole(String label) {
        this.label = label;
    }

    // This stays exactly the same and still works perfectly!
    public static List<Map<String, String>> getDropdownValues() {
        return Arrays.stream(values())
                .map(status -> Map.of(
                        "value", status.name(), // will output "assistant" or "user"
                        "label", status.getLabel() // will output "Assistant" or "User"
                ))
                .collect(Collectors.toList());
    }
}
