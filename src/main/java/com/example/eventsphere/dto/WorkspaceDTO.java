package com.example.eventsphere.dto;

import com.example.eventsphere.enums.AppStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class WorkspaceDTO {

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
