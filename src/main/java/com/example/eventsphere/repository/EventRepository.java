package com.example.eventsphere.repository;

import com.example.eventsphere.dto.EventDTO;
import com.example.eventsphere.dto.EventListDTO;
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
            w.name, 
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
        JOIN Workspace w ON e.workspaceId = w.id
        JOIN Category c ON e.categoryId = c.id
        WHERE e.id = :eventId
        """)
    Optional<EventDTO> findAdminEventDetailsById(@Param("eventId") UUID eventId);
}
