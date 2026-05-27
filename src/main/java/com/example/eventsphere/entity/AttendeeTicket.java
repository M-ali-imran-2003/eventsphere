package com.example.eventsphere.entity;

import com.example.eventsphere.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attendee_tickets")
@Data
public class AttendeeTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ticket_id")
    private UUID id;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "tier_id")
    private UUID tierId;

    @Column(name = "assigned_name")
    private String assignedName;

    @Column(name = "assigned_email")
    private String assignedEmail;

    @Column(name = "assigned_phone")
    private String assignedPhone;

    @Column(name = "assigned_cnic")
    private String assignedCnic;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "qr_code_hash")
    private String qrCodeHash;

    @Column(name = "is_checked_in")
    private boolean isCheckedIn;

    @Column(name = "check_in_timestamp")
    private LocalDateTime checkInTime;

}
