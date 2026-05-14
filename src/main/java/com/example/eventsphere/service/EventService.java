package com.example.eventsphere.service;


import com.example.eventsphere.dto.*;
import com.example.eventsphere.entity.*;
import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.EventStatus;
import com.example.eventsphere.enums.FileType;
import com.example.eventsphere.enums.OrgRole;
import com.example.eventsphere.mapper.GenericMapper;
import com.example.eventsphere.repository.*;
import com.example.eventsphere.utils.LocationUtil;
import com.example.eventsphere.utils.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
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
    private final SubEventRepository subEventRepository;
    private final TicketTierRepository ticketTierRepository;


    @Autowired
    public EventService(EventRepository eventRepository, GenericMapper mapper, OrganizationMemberRepository organizationMemberRepository, OrganizationRepository organizationRepository, FileService fileService, CategoryRepository categoryRepository, SubEventRepository subEventRepository, TicketTierRepository ticketTierRepository) {
        this.eventRepository = eventRepository;
        this.mapper = mapper;
        this.organizationMemberRepository = organizationMemberRepository;
        this.organizationRepository = organizationRepository;
        this.fileService = fileService;
        this.categoryRepository = categoryRepository;
        this.subEventRepository = subEventRepository;
        this.ticketTierRepository = ticketTierRepository;
    }

    public List<EventListDTO> getAllEventsForAdmin(){
        return mapper.mapList(eventRepository.findAll(),EventListDTO.class);
    }

    public EventDTO getEventByIdForAdmin(UUID id){

        return eventRepository.findEventDetailsById(id).orElseThrow(()-> new RuntimeException("Event Not Found With ID: "+ id));

    }

    public List<EventMapMarkerDTO> getAllEventsLocation(){

        return eventRepository.findAllEventLocationsForMap();

    }

    public EventDTO findEvent(UUID eventId){
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


        return eventRepository.findEventDetailsById(eventId).orElseThrow(() -> new RuntimeException("Event not found"));

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
    public void updateEventDetails(UUID eventId, UpdateEventRequest request) {
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
        if (request.getDescription() != null && !request.getDescription().isBlank()) event.setDescription(request.getDescription());
        if(request.getLat() != null && request.getLon() != null) event.setLocation(LocationUtil.createPoint(request.getLat(),request.getLon()));
        if (request.getVenue() != null && !request.getVenue().isBlank()) event.setVenue(request.getVenue());
        if (request.getAddress() != null && !request.getAddress().isBlank()) event.setAddress(request.getAddress());
        if (request.getCity() != null&& !request.getCity().isBlank()) event.setCity(request.getCity());
        if (request.getState() != null&& !request.getState().isBlank()) event.setState(request.getState());
        if (request.getCountry() != null&& !request.getCountry().isBlank()) event.setCountry(request.getCountry());
        if (request.getLayoutType() != null&& !request.getLayoutType().isBlank()) event.setLayoutType(request.getLayoutType());
        if (request.getTags() != null&& !request.getTags().isEmpty()) event.setTags(request.getTags());
        if (request.getTypeSpecificData() != null && !request.getTypeSpecificData().isBlank()) event.setTypeSpecificData(request.getTypeSpecificData());

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

        event.setModifiedBy(currentUser.getId());

        eventRepository.save(event);
        log.info("Event '{}' updated successfully by User ID: {}", event.getTitle(), currentUser.getId());


    }

    @Transactional
    public TicketTier addTicketTier(UUID eventId, CreateTicketTierRequest request) {
        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) throw new RuntimeException("Unauthorized.");

        // 1. Fetch Event & Verify Ownership
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found."));

        OrganizationMember member = organizationMemberRepository
                .findByOrganizationIdAndUserId(event.getOrganizationId(), currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Access denied."));

        if (member.getRole() != OrgRole.OWNER) {
            throw new RuntimeException("Only Organization Owners can manage ticketing.");
        }

        // 2. Business Validation: No duplicate tier names
        if (ticketTierRepository.existsByEventIdAndTierNameIgnoreCase(eventId, request.getTierName())) {
            throw new RuntimeException("A ticket tier with this name already exists for this event.");
        }

        // 3. Create and Save
        TicketTier tier = new TicketTier();
        tier.setEventId(eventId);
        tier.setTierName(request.getTierName());
        tier.setPrice(request.getPrice());
        tier.setTotalCapacity(request.getTotalCapacity());
        tier.setCreatedBy(currentUser.getId());
        tier.setModifiedBy(currentUser.getId());


        log.info("Ticket Tier '{}' added to Event {} by User {}", tier.getTierName(), eventId, currentUser.getId());
        return ticketTierRepository.save(tier);
    }

    public List<TicketTier> getTicketTiersByEventId(UUID eventId) {
        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) throw new RuntimeException("Unauthorized.");
        verifyEventOwnership(eventId,currentUser);
        return ticketTierRepository.findByEventId(eventId);
    }

    public TicketTier getTicketTierById(UUID ticketId) {

        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) throw new RuntimeException("Unauthorized.");

       TicketTier ticketTier = ticketTierRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket Tier not found."));

       verifyEventOwnership(ticketTier.getEventId(),currentUser);
       return ticketTier;
    }

    @Transactional
    public TicketTier updateTicketTier(UUID eventId, UUID ticketId, UpdateTicketTierRequest request) {
        User currentUser = SecurityUtil.getCurrentUser();

        if (currentUser == null) throw new RuntimeException("Unauthorized.");

        verifyEventOwnership(eventId, currentUser);

        TicketTier tier = getTicketTierById(ticketId);

        if (request.getTierName() != null && !tier.getTierName().equals(request.getTierName())) {
            if (ticketTierRepository.existsByEventIdAndTierNameIgnoreCase(eventId, request.getTierName())) {
                throw new RuntimeException("A ticket tier with this name already exists.");
            }
            tier.setTierName(request.getTierName());
        }

        if (request.getPrice() != null ) tier.setPrice(request.getPrice());

        if (request.getTotalCapacity() != null) {
            if (request.getTotalCapacity() < tier.getQuantitySold()) {
                throw new RuntimeException("Cannot lower capacity below the number of tickets already sold.");
            }
            tier.setTotalCapacity(request.getTotalCapacity());
        }

        tier.setModifiedBy(currentUser.getId());


        return ticketTierRepository.save(tier);
    }

    @Transactional
    public void deleteTicketTier(UUID eventId, UUID ticketId) {
        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) throw new RuntimeException("Unauthorized.");

        verifyEventOwnership(eventId, currentUser);

        TicketTier tier = getTicketTierById(ticketId);

        // Crucial Business Logic: Prevent deleting a tier if people paid for it
        if (tier.getQuantitySold() > 0) {
            throw new RuntimeException("Cannot delete this ticket tier because tickets have already been sold.");
        }

        ticketTierRepository.delete(tier);
        log.info("Ticket Tier '{}' deleted successfully.", tier.getTierName());
    }


    @Transactional
    public SubEvent addSubEvent(UUID eventId, CreateSubEventRequest request) {
        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) throw new RuntimeException("Unauthorized.");

        // 1. Fetch Event & Verify Ownership
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found."));

        OrganizationMember member = organizationMemberRepository
                .findByOrganizationIdAndUserId(event.getOrganizationId(), currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Access denied."));

        if (member.getRole() != OrgRole.OWNER) {
            throw new RuntimeException("Only Organization Owners can manage the agenda.");
        }

        // 2. Validate Time Logic
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new RuntimeException("Sub-event end time cannot be before the start time.");
        }

        // Optional: Ensure sub-event dates fall within the main event dates
        if (request.getStartTime().isBefore(event.getStartDateTime()) ||
                request.getEndTime().isAfter(event.getEndDateTime())) {
            throw new RuntimeException("Sub-event times must fall within the main event's duration.");
        }

        // 3. Create the SubEvent
        SubEvent subEvent = new SubEvent();
        subEvent.setEventId(eventId);
        subEvent.setTitle(request.getTitle());
        subEvent.setDescription(request.getDescription());
        subEvent.setStartTime(request.getStartTime());
        subEvent.setEndTime(request.getEndTime());
        subEvent.setRoomOrLocation(request.getRoomOrLocation());
        subEvent.setCapacityLimit(request.getCapacityLimit());
        subEvent.setRulesConfig(request.getRulesConfig());
        subEvent.setCreatedBy(currentUser.getId());
        subEvent.setModifiedBy(currentUser.getId());

        // 4. Handle Image Upload (Speaker Headshot / Map)
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            try {
                // Adjust FileType enum based on your actual file service implementation
                String imageUrl = fileService.saveFile(request.getImage(), FileType.IMAGE);
                subEvent.setImageUrl(imageUrl);
            } catch (Exception e) {
                log.error("Failed to save sub-event image", e);
                throw new RuntimeException("Failed to upload image. Please try again.");
            }
        }

        log.info("Sub-event '{}' added to Event {} by User {}", subEvent.getTitle(), eventId, currentUser.getId());
        return subEventRepository.save(subEvent);
    }

    public List<SubEvent> getSubEventsByEventId(UUID eventId) {
        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) throw new RuntimeException("Unauthorized.");

        verifyEventOwnership(eventId, currentUser);
        return subEventRepository.findByEventIdOrderByStartTimeAsc(eventId);
    }

    public SubEvent getSubEventById(UUID subEventId) {
        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) throw new RuntimeException("Unauthorized.");
        SubEvent subEvent = subEventRepository.findById(subEventId)
                .orElseThrow(() -> new RuntimeException("Sub Event not found."));
        verifyEventOwnership(subEvent.getEventId(), currentUser);

        return subEvent;
    }

    @Transactional
    public SubEvent updateSubEvent(UUID eventId, UUID subEventId, UpdateSubEventRequest request) {
        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) throw new RuntimeException("Unauthorized.");

        verifyEventOwnership(eventId, currentUser);

        SubEvent subEvent = getSubEventById(subEventId);


        Event event = eventRepository.findById(subEvent.getEventId()).orElseThrow(() -> new RuntimeException("Event not found."));

        if (request.getStartTime().isBefore(event.getStartDateTime()) ||
                request.getEndTime().isAfter(event.getEndDateTime())) {
            throw new RuntimeException("Sub-event times must fall within the main event's duration.");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) subEvent.setTitle(request.getTitle());
        if (request.getDescription() != null&& !request.getDescription().isBlank())  subEvent.setDescription(request.getDescription());
        if (request.getStartTime() != null) subEvent.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) subEvent.setEndTime(request.getEndTime());
        if (request.getRoomOrLocation() != null&& !request.getRoomOrLocation().isBlank()) subEvent.setRoomOrLocation(request.getRoomOrLocation());
        if (request.getCapacityLimit() != null) subEvent.setCapacityLimit(request.getCapacityLimit());
        if (request.getRulesConfig() != null && !request.getRulesConfig().isBlank()) subEvent.setRulesConfig(request.getRulesConfig());

        if (subEvent.getEndTime().isBefore(subEvent.getStartTime())) {
            throw new RuntimeException("End time cannot be before start time.");
        }

        // Handle File Update & Cleanup
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String oldFilePath = subEvent.getImageUrl();
            try {
                // Adjust FileType based on your enum
                String newImageUrl = fileService.saveFile(request.getImage(), FileType.IMAGE);
                subEvent.setImageUrl(newImageUrl);

                if (oldFilePath != null && !oldFilePath.isBlank()) {
                    try {
                        fileService.deleteFile(oldFilePath);
                    } catch (Exception e) {
                        log.error("Could not delete old sub-event image: {}", oldFilePath);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to update image.");
            }
        }

        subEvent.setModifiedBy(currentUser.getId());

        return subEventRepository.save(subEvent);
    }

    @Transactional
    public void deleteSubEvent(UUID eventId, UUID subEventId) {
        User currentUser = SecurityUtil.getCurrentUser();
        verifyEventOwnership(eventId, currentUser);

        SubEvent subEvent = getSubEventById(subEventId);

        // Delete associated image from storage
        if (subEvent.getImageUrl() != null && !subEvent.getImageUrl().isBlank()) {
            try {
                fileService.deleteFile(subEvent.getImageUrl());
            } catch (Exception e) {
                log.error("Failed to delete sub-event image during deletion: {}", subEvent.getImageUrl());
            }
        }

        subEventRepository.delete(subEvent);
    }

    private void verifyEventOwnership(UUID eventId, User user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found."));

        Organization org = organizationRepository.findById(event.getOrganizationId())
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        if(!org.getStatus().equals(AppStatus.ACTIVE)) {
            throw new RuntimeException("Organization is not active");
        }

        OrganizationMember member = organizationMemberRepository
                .findByOrganizationIdAndUserId(org.getId(), user.getId())
                .orElseThrow(() -> new RuntimeException("Access denied."));


        if (member.getRole() != OrgRole.OWNER) {
            throw new RuntimeException("Only Organization Owners can manage tickets.");
        }
    }
}
