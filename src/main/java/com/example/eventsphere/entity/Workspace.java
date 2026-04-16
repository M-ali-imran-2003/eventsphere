package com.example.eventsphere.entity;


import com.example.eventsphere.enums.AppStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workspace")
@Data
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "workspace_id")
    private UUID id;

    @NotBlank(message = "Name is required")
    @Size(min=5,max = 50, message = "Name must be in 5-50 characters")
    @Column(name = "workspace_name")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email")
    @Size(max = 30, message = "Email must be under 30 characters")
    @Column(name = "email")
    private String email;

    @Column(name = "image_url")
    private String image_url;

    @Column(name = "pending_balance",precision = 10, scale = 2)
    private BigDecimal pendingBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AppStatus status;

    @Column(name = "current_plan_id")
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status")
    private AppStatus subscriptionStatus;

    @Column(name = "subscription_end_date")
    private LocalDateTime subscriptionEndDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by")
    private UUID modifiedBy;
}
