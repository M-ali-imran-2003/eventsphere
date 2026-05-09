package com.example.eventsphere.repository;

import com.example.eventsphere.dto.EventDTO;
import com.example.eventsphere.dto.EventMapMarkerDTO;
import com.example.eventsphere.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query("""
        SELECT new com.example.eventsphere.dto.EventDTO(
            e.id, 
            o.name, 
            c.name, 
            e.title, 
            e.address, 
            e.city, 
            e.state, 
            e.country, 
            e.location, 
            e.startDateTime, 
            e.endDateTime, 
            e.status, 
            e.createdAt, 
            e.createdBy, 
            e.modifiedAt, 
            e.modifiedBy
        )
        FROM Event e
        JOIN Organization o ON e.organizationId = o.id
        JOIN Category c ON e.categoryId = c.id
        WHERE e.id = :eventId
        """)
    Optional<EventDTO> findAdminEventDetailsById(@Param("eventId") UUID eventId);

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
}
