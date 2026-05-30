package com.example.eventsphere.repository;

import com.example.eventsphere.entity.AttendeeTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendeeTicketRepository extends JpaRepository<AttendeeTicket, UUID> {

    // Fetches all tickets where this user is the assigned attendee
    List<AttendeeTicket> findByAssignedEmail(String email);

    Optional<List<AttendeeTicket>> findByOrderId(UUID orderId);

}
