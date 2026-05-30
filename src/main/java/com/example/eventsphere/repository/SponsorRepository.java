package com.example.eventsphere.repository;

import com.example.eventsphere.entity.Sponsor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SponsorRepository extends JpaRepository<Sponsor, UUID> {

    // The frontend will need to fetch all sponsors for a specific event
    List<Sponsor> findByEventId(UUID eventId);

    // Optional: Fetch by event and order by tier (e.g., Platinum first)
    List<Sponsor> findByEventIdOrderBySponsorTierAsc(UUID eventId);
}