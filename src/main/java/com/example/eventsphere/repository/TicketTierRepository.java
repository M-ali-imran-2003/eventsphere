package com.example.eventsphere.repository;

import com.example.eventsphere.entity.TicketTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketTierRepository extends JpaRepository<TicketTier,UUID> {

    List<TicketTier> findByEventId(UUID eventId);

    boolean existsByEventIdAndTierNameIgnoreCase(UUID eventId, String tierName);

}
