package com.example.eventsphere.event;

import java.util.UUID;

public record RagIndexRequestedEvent(
        UUID eventId
) {
}