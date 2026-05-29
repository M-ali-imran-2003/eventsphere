package com.example.eventsphere.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "ticket_sub_event_registrations")
@Data
@IdClass(SubEventRegistrationId.class)
public class SubEventRegistration {

    @Id
    @Column(name = "ticket_id")
    private UUID ticketId;

    @Id
    @Column(name = "sub_event_id")
    private UUID subEventId;
}