package com.example.eventsphere.repository;

import com.example.eventsphere.dto.EventDTO;
import com.example.eventsphere.dto.EventMapMarkerDTO;
import com.example.eventsphere.dto.PublicEventCardDTO;
import com.example.eventsphere.entity.Event;
import com.example.eventsphere.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findByStartDateTimeBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

    @Query("""
        SELECT new com.example.eventsphere.dto.EventDTO(
            e.id, 
            o.name, 
            c.name, 
            e.title,
            e.description,
            e.address, 
            e.venue,
            e.imageUrl, 
            e.city, 
            e.state, 
            e.country, 
            e.location, 
            e.startDateTime, 
            e.endDateTime, 
            e.status,
            e.layoutType, 
            e.tags, 
            e.typeSpecificData, 
            e.createdAt, 
            uc.username, 
            e.modifiedAt, 
            um.username
        )
        FROM Event e
        JOIN Organization o ON e.organizationId = o.id
        JOIN Category c ON e.categoryId = c.id
        LEFT JOIN User uc ON e.createdBy = uc.id
        LEFT JOIN User um ON e.modifiedBy = um.id
        WHERE e.organizationId = :organizationId
        """)
    List<EventDTO> findCurrentOrganizationEvents(@Param("organizationId") UUID organizationId);

    @Query("""
        SELECT new com.example.eventsphere.dto.EventDTO(
            e.id, 
            o.name, 
            c.name, 
            e.title,
            e.description,
            e.address, 
            e.venue,
            e.imageUrl, 
            e.city, 
            e.state, 
            e.country, 
            e.location, 
            e.startDateTime, 
            e.endDateTime, 
            e.status,
            e.layoutType, 
            e.tags, 
            e.typeSpecificData, 
            e.createdAt, 
            uc.username, 
            e.modifiedAt, 
            um.username
        )
        FROM Event e
        JOIN Organization o ON e.organizationId = o.id
        JOIN Category c ON e.categoryId = c.id
        LEFT JOIN User uc ON e.createdBy = uc.id
        LEFT JOIN User um ON e.modifiedBy = um.id
        WHERE e.id = :eventId
        """)
    Optional<EventDTO> findEventDetailsById(@Param("eventId") UUID eventId);

    @Query("""
    SELECT new com.example.eventsphere.dto.EventMapMarkerDTO(
        e.id, 
        e.title, 
        o.name, 
        e.location, 
        e.status
    )
    FROM Event e
    JOIN Organization o ON e.organizationId = o.id
    WHERE e.location IS NOT NULL
    """)
    List<EventMapMarkerDTO> findAllEventLocationsForMap();

    boolean existsByTitleAndOrganizationId(String title, UUID organizationId);

    @Query("""
        SELECT new com.example.eventsphere.dto.PublicEventCardDTO(
            e.title,
            o.name,
            c.name,
            l.slug,
            e.imageUrl,
            e.startDateTime,
            e.venue,
            e.city,
            e.state,
            e.country,
            e.tags
        )
        FROM Event e
        JOIN Organization o ON e.organizationId = o.id
        JOIN Category c ON e.categoryId = c.id
        JOIN LandingPage l ON e.id = l.eventId
        WHERE e.status = :status
        ORDER BY e.startDateTime ASC
        """)
    List<PublicEventCardDTO> findByStatusOrderByStartDatetimeAsc(EventStatus status);
}
