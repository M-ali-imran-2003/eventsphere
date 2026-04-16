package com.example.eventsphere.dto;

import com.example.eventsphere.enums.AppStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class WorkspaceListDTO {

    private UUID id;

    private String name;

    private String email;

    private AppStatus status;

    private LocalDateTime subscriptionEndDate;

}
