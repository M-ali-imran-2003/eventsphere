package com.example.eventsphere.repository;

import com.example.eventsphere.entity.LandingPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LandingPageRepository extends JpaRepository<LandingPage, UUID> {

    // For the Organizer: Find their specific page by their Event ID
    Optional<LandingPage> findByEventId(UUID eventId);
    List<LandingPage> findByEventIdIn(Collection<UUID> eventIds);
    // For the Public Web: Find the page when a user clicks a custom URL link
    Optional<LandingPage> findBySlug(String slug);

    // Validation: Check if a URL is already taken before saving
    boolean existsBySlug(String slug);
}