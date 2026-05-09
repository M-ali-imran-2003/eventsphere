package com.example.eventsphere.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateEventRequest {

    @NotBlank(message = "Event title is required")
    private String title;

    @NotNull(message = "Category is required")
    private UUID categoryId;

    @NotNull(message = "Start date and time are required")
    @Future(message = "Start date must be in the future")
    private LocalDateTime startDatetime;

    @NotNull(message = "End date and time are required")
    @Future(message = "End date must be in the future")
    private LocalDateTime endDatetime;
}
