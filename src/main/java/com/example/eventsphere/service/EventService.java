package com.example.eventsphere.service;


import com.example.eventsphere.dto.CreateEventRequest;
import com.example.eventsphere.dto.EventDTO;
import com.example.eventsphere.dto.EventListDTO;
import com.example.eventsphere.dto.EventMapMarkerDTO;
import com.example.eventsphere.entity.Event;
import com.example.eventsphere.entity.OrganizationMember;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.EventStatus;
import com.example.eventsphere.enums.OrgRole;
import com.example.eventsphere.mapper.GenericMapper;
import com.example.eventsphere.repository.CategoryRepository;
import com.example.eventsphere.repository.EventRepository;
import com.example.eventsphere.repository.OrganizationMemberRepository;
import com.example.eventsphere.utils.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final GenericMapper mapper;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final CategoryRepository categoryRepository;

    @Autowired
    public EventService(EventRepository eventRepository, GenericMapper mapper, OrganizationMemberRepository organizationMemberRepository, CategoryRepository categoryRepository) {
        this.eventRepository = eventRepository;
        this.mapper = mapper;
        this.organizationMemberRepository = organizationMemberRepository;
        this.categoryRepository = categoryRepository;
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

    @Transactional
    public Event createDraftEvent(UUID organizationId, CreateEventRequest request) {
        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("Unauthorized. Please log in.");
        }

        // SECURITY CHECK 1: Ensure the user belongs to this organization
        // (If you add 'MANAGER' roles later, you can expand this check)
        OrganizationMember member = organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("You do not have access to this organization."));

        if (member.getRole() != OrgRole.OWNER) {
            log.warn("SECURITY ALERT: User {} attempted to create an event without OWNER permissions.", currentUser.getId());
            throw new RuntimeException("Only Organization Owners can create new events.");
        }

        // SECURITY CHECK 2: Validate the Category exists
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new RuntimeException("The selected category does not exist.");
        }

        // SECURITY CHECK 3: Prevent duplicate event names in the same organization
        if (eventRepository.existsByTitleAndOrganizationId(request.getTitle(), organizationId)) {
            throw new RuntimeException("An event with this title already exists in your organization.");
        }

        // VALIDATION: Ensure End Date is after Start Date
        if (request.getEndDatetime().isBefore(request.getStartDatetime())) {
            throw new RuntimeException("Event end time cannot be before the start time.");
        }

        // Create the Event shell
        Event newEvent = new Event();
        newEvent.setOrganizationId(organizationId);
        newEvent.setCategoryId(request.getCategoryId());
        newEvent.setTitle(request.getTitle());
        newEvent.setStartDateTime(request.getStartDatetime());
        newEvent.setEndDateTime(request.getEndDatetime());

        // System defaults for a brand new event
        newEvent.setStatus(EventStatus.DRAFT); // Keeping it as a Draft
        newEvent.setCreatedBy(currentUser.getId());
        newEvent.setModifiedBy(currentUser.getId());

        Event savedEvent = eventRepository.save(newEvent);
        log.info("Draft Event '{}' created successfully by User ID: {}", savedEvent.getTitle(), currentUser.getId());

        return savedEvent;
    }
}
