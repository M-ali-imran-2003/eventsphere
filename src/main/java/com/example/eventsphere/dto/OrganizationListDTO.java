package com.example.eventsphere.dto;

import com.example.eventsphere.enums.AppStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class OrganizationListDTO {

    private UUID id;

    private String name;

    private String email;

    private AppStatus status;

    private LocalDateTime subscriptionEndDate;

}
