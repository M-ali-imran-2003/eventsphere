package com.example.eventsphere.service;


import com.example.eventsphere.dto.*;
import com.example.eventsphere.entity.Event;
import com.example.eventsphere.entity.Organization;
import com.example.eventsphere.entity.OrganizationMember;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.EventStatus;
import com.example.eventsphere.enums.FileType;
import com.example.eventsphere.enums.OrgRole;
import com.example.eventsphere.mapper.GenericMapper;
import com.example.eventsphere.repository.CategoryRepository;
import com.example.eventsphere.repository.EventRepository;
import com.example.eventsphere.repository.OrganizationMemberRepository;
import com.example.eventsphere.repository.OrganizationRepository;
import com.example.eventsphere.utils.LocationUtil;
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
    private final OrganizationRepository organizationRepository;
    private final FileService fileService;
    private final CategoryRepository categoryRepository;

    @Autowired
    public EventService(EventRepository eventRepository, GenericMapper mapper, OrganizationMemberRepository organizationMemberRepository, OrganizationRepository organizationRepository, FileService fileService, CategoryRepository categoryRepository) {
        this.eventRepository = eventRepository;
        this.mapper = mapper;
        this.organizationMemberRepository = organizationMemberRepository;
        this.organizationRepository = organizationRepository;
        this.fileService = fileService;
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

    public Event findEvent(UUID eventId){
        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("Unauthorized. Please log in.");
        }

        Event event = eventRepository.findById(eventId).orElseThrow(() -> new RuntimeException("Event not found"));

        Organization org = organizationRepository.findById(event.getOrganizationId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        if(!org.getStatus().equals(AppStatus.ACTIVE)) {
            throw new RuntimeException("Organization is not active");
        }

        OrganizationMember member = organizationMemberRepository.findByOrganizationIdAndUserId(event.getOrganizationId(), currentUser.getId())
                .orElseThrow(() -> new RuntimeException("You do not have access to this organization."));

        return event;

    }

    public List<EventDTO> getCurrentOrganizationEvents(UUID organizationId){

        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("Unauthorized. Please log in.");
        }

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        if(!org.getStatus().equals(AppStatus.ACTIVE)) {
            throw new RuntimeException("Organization is not active");
        }

        OrganizationMember member = organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("You do not have access to this organization."));

        return eventRepository.findCurrentOrganizationEvents(organizationId);
    }

    @Transactional
    public Event createDraftEvent(UUID organizationId, CreateEventRequest request) {
        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("Unauthorized. Please log in.");
        }

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        if(!org.getStatus().equals(AppStatus.ACTIVE)) {
            throw new RuntimeException("Organization is not active");
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

    @Transactional
    public Event updateEventDetails(UUID eventId, UpdateEventRequest request) {
        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("Unauthorized. Please log in.");
        }

        // 1. Fetch the Event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found."));

        // 2. SECURITY CHECK: Verify they own the Organization that owns this Event
        OrganizationMember member = organizationMemberRepository
                .findByOrganizationIdAndUserId(event.getOrganizationId(), currentUser.getId())
                .orElseThrow(() -> new RuntimeException("You do not have access to this organization."));

        Organization org = organizationRepository.findById(event.getOrganizationId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        if(!org.getStatus().equals(AppStatus.ACTIVE)) {
            throw new RuntimeException("Organization is not active");
        }

        if (member.getRole() != OrgRole.OWNER) {
            throw new RuntimeException("Only Organization Owners can edit events.");
        }

        // 3. Update Core Fields (With Validation)
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            // Prevent renaming to an existing event's title
            if (!event.getTitle().equals(request.getTitle()) &&
                    eventRepository.existsByTitleAndOrganizationId(request.getTitle(), event.getOrganizationId())) {
                throw new RuntimeException("An event with this title already exists in your organization.");
            }
            event.setTitle(request.getTitle());
        }

        if (request.getCategoryId() != null) {
            if (!categoryRepository.existsById(request.getCategoryId())) {
                throw new RuntimeException("The selected category does not exist.");
            }
            event.setCategoryId(request.getCategoryId());
        }

        if (request.getStartDatetime() != null) event.setStartDateTime(request.getStartDatetime());
        if (request.getEndDatetime() != null) event.setEndDateTime(request.getEndDatetime());

        if (event.getEndDateTime().isBefore(event.getStartDateTime())) {
            throw new RuntimeException("Event end time cannot be before the start time.");
        }

        // 4. Update Text & Metadata Fields
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if(request.getLat() != null && request.getLon() != null) event.setLocation(LocationUtil.createPoint(request.getLat(),request.getLon()));
        if (request.getVenue() != null) event.setVenue(request.getVenue());
        if (request.getAddress() != null) event.setAddress(request.getAddress());
        if (request.getCity() != null) event.setCity(request.getCity());
        if (request.getState() != null) event.setState(request.getState());
        if (request.getCountry() != null) event.setCountry(request.getCountry());
        if (request.getLayoutType() != null) event.setLayoutType(request.getLayoutType());
        if (request.getTags() != null) event.setTags(request.getTags());
        if (request.getTypeSpecificData() != null) event.setTypeSpecificData(request.getTypeSpecificData());

        // 5. Handle the Image Upload with Ghost-File Cleanup
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String oldFilePath = event.getImageUrl();
            try {
                // Assuming FileType.IMAGE exists in your enum
                String newImageUrl = fileService.saveFile(request.getImage(), FileType.IMAGE);
                event.setImageUrl(newImageUrl);

                if (oldFilePath != null && !oldFilePath.isBlank()) {
                    try {
                        fileService.deleteFile(oldFilePath);
                    } catch (Exception e) {
                        log.error("Could not delete old event image for {}. File path: {}", event.getTitle(), oldFilePath, e);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to save event picture", e);
                throw new RuntimeException("Failed to upload the event image. Please try again.");
            }
        }

        log.info("Event '{}' updated successfully by User ID: {}", event.getTitle(), currentUser.getId());
        return eventRepository.save(event);
    }
}
