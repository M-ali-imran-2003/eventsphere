package com.example.eventsphere.service;


import com.example.eventsphere.dto.EventDTO;
import com.example.eventsphere.dto.EventListDTO;
import com.example.eventsphere.dto.EventMapMarkerDTO;
import com.example.eventsphere.mapper.GenericMapper;
import com.example.eventsphere.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final GenericMapper mapper;

    @Autowired
    public EventService(EventRepository eventRepository, GenericMapper mapper) {
        this.eventRepository = eventRepository;
        this.mapper = mapper;
    }

    public List<EventListDTO> getAllEventsForAdmin(){
        return mapper.mapList(eventRepository.findAll(),EventListDTO.class);
    }

    public EventDTO getEventByIdForAdmin(UUID id){

        return eventRepository.findAdminEventDetailsById(id).orElseThrow(()-> new RuntimeException("Event Not Found With ID: "+ id));

    }

    public List<EventMapMarkerDTO> getAllEventsLocation(){

        return eventRepository.findAllEventLocationsForMap();

    }


}
