package com.example.eventsphere.dto;

import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventListDTO {

    private UUID id;

    private String title;

    private String city;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private EventStatus Status;

}
