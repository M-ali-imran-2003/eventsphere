package com.example.eventsphere.repository;

import com.example.eventsphere.entity.AttendeeTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendeeTicketRepository extends JpaRepository<AttendeeTicket, UUID> {

    // Fetches all tickets where this user is the assigned attendee
    List<AttendeeTicket> findByAssignedEmail(String email);

    Optional<List<AttendeeTicket>> findByOrderId(UUID orderId);

    @Query(
            "SELECT DISTINCT t.assignedEmail FROM AttendeeTicket t " +
                    "JOIN Order o ON t.orderId = o.id " +
                    "WHERE o.eventId = :eventId AND o.paymentStatus = 'SUCCESS'"
    )
    List<String> findDistinctEmailsByEventId(@Param("eventId") UUID eventId);
}
