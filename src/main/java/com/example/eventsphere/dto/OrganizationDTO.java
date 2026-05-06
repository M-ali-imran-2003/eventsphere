package com.example.eventsphere.dto;

import com.example.eventsphere.enums.AppStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class OrganizationDTO {

    private UUID id;

    private String name;

    private String email;

    private String image_url;

    private BigDecimal pendingBalance;

    private AppStatus status;

    private String planName;

    private AppStatus subscriptionStatus;

    private LocalDateTime subscriptionEndDate;

    private LocalDateTime createdAt;

    private UUID createdBy;

    private LocalDateTime modifiedAt;

    private UUID modifiedBy;
}
