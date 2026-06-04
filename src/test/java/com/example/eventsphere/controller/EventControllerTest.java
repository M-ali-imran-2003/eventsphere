package com.example.eventsphere.controller;

import com.example.eventsphere.dto.EventDTO;
import com.example.eventsphere.dto.EventListDTO;
import com.example.eventsphere.dto.EventMapMarkerDTO;
import com.example.eventsphere.repository.EmailBroadcastHistoryRepository;
import com.example.eventsphere.service.EventService;
import com.example.eventsphere.filter.JwtAuthenticationFilter;
import com.example.eventsphere.service.OrderService;
import com.example.eventsphere.service.TicketService;
import com.example.eventsphere.utils.JwtUtil;
import com.example.eventsphere.filter.RequestLoggingFilter;
import com.example.eventsphere.utils.SecurityUtil;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private EmailBroadcastHistoryRepository emailBroadcastHistoryRepository;

    // === MOCKS TO PREVENT SECURITY FILTER CRASHES ===
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private RequestLoggingFilter requestLoggingFilter;
    @MockitoBean private SecurityUtil securityUtil;
    // ================================================

    @Test
    void getAllEvents_ShouldReturn200AndList() throws Exception {
        when(eventService.getAllEventsForAdmin()).thenReturn(Collections.singletonList(new EventListDTO()));

        mockMvc.perform(get("/api/event/get-all-events")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getEventById_ShouldReturn200AndEventDetails() throws Exception {
        UUID testId = UUID.randomUUID();
        when(eventService.getEventByIdForAdmin(testId)).thenReturn(new EventDTO());

        mockMvc.perform(get("/api/event/get-event-by-id/" + testId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getEventById_ShouldReturn400BadRequest_WhenEventNotFound() throws Exception {
        UUID testId = UUID.randomUUID();

        // This proves your GlobalExceptionHandler works for the Event module too!
        when(eventService.getEventByIdForAdmin(testId))
                .thenThrow(new RuntimeException("Event Not Found With ID: " + testId));

        mockMvc.perform(get("/api/event/get-event-by-id/" + testId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Event Not Found With ID: " + testId));
    }

    @Test
    void getAllEventsLocationMap_ShouldReturn200AndMarkers() throws Exception {
        when(eventService.getAllEventsLocation()).thenReturn(Collections.singletonList(new EventMapMarkerDTO()));

        mockMvc.perform(get("/api/event/get-all-events-location-map")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}