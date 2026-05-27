package com.example.eventsphere.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "ticket_sub_event_registrations")
@Data
public class SubEventRegistration {
    @Column(name = "ticket_id")
    private UUID ticketId;

    @Column(name = "sub_event_id")
    private UUID subEventId;
}
