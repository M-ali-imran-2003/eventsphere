package com.example.eventsphere.service;

import com.example.eventsphere.dto.EventDTO;
import com.example.eventsphere.dto.EventListDTO;
import com.example.eventsphere.dto.EventMapMarkerDTO;
import com.example.eventsphere.mapper.GenericMapper;
import com.example.eventsphere.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private GenericMapper mapper;

    @InjectMocks
    private EventService eventService;

    @Test
    void getAllEventsForAdmin_ShouldReturnListOfEvents() {
        // GIVEN
        when(eventRepository.findAll()).thenReturn(Collections.emptyList());
        when(mapper.mapList(anyList(), eq(EventListDTO.class))).thenReturn(Collections.singletonList(new EventListDTO()));

        // WHEN
        List<EventListDTO> result = eventService.getAllEventsForAdmin();

        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository, times(1)).findAll();
    }

    @Test
    void getEventByIdForAdmin_ShouldReturnEvent_WhenIdExists() {
        // GIVEN
        UUID eventId = UUID.randomUUID();
        EventDTO mockEvent = new EventDTO();
        when(eventRepository.findAdminEventDetailsById(eventId)).thenReturn(Optional.of(mockEvent));

        // WHEN
        EventDTO result = eventService.getEventByIdForAdmin(eventId);

        // THEN
        assertNotNull(result);
        verify(eventRepository, times(1)).findAdminEventDetailsById(eventId);
    }

    @Test
    void getEventByIdForAdmin_ShouldThrowException_WhenIdDoesNotExist() {
        // GIVEN
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findAdminEventDetailsById(eventId)).thenReturn(Optional.empty());

        // WHEN & THEN
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            eventService.getEventByIdForAdmin(eventId);
        });

        assertEquals("Event Not Found With ID: " + eventId, exception.getMessage());
    }

    @Test
    void getAllEventsLocation_ShouldReturnMapMarkers() {
        // GIVEN
        when(eventRepository.findAllEventLocationsForMap()).thenReturn(Collections.singletonList(new EventMapMarkerDTO()));

        // WHEN
        List<EventMapMarkerDTO> result = eventService.getAllEventsLocation();

        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository, times(1)).findAllEventLocationsForMap();
    }
}