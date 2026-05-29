package com.example.eventsphere.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubEventRegistrationId implements Serializable {

    private UUID ticketId;
    private UUID subEventId;
}