package com.example.eventsphere.service;

import com.example.eventsphere.dto.*;
import com.example.eventsphere.entity.Organization;
import com.example.eventsphere.entity.OrganizationMember;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.FileType;
import com.example.eventsphere.enums.OrgRole;
import com.example.eventsphere.enums.UserRole;
import com.example.eventsphere.mapper.GenericMapper;
import com.example.eventsphere.repository.OrganizationMemberRepository;
import com.example.eventsphere.repository.OrganizationRepository;
import com.example.eventsphere.utils.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class OrganizationService {

    private final GenericMapper mapper;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final FileService fileService;

    @Autowired
    public OrganizationService(GenericMapper mapper, OrganizationRepository organizationRepository, OrganizationMemberRepository organizationMemberRepository, FileService fileService) {
        this.mapper = mapper;
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.fileService = fileService;
    }

    public List<OrganizationListDTO> getAllOrganization() {
        return mapper.mapList(organizationRepository.findAll(), OrganizationListDTO.class);
    }

    public OrganizationDTO getOrganizationById(UUID id) {
        Organization organization = organizationRepository.findById(id).orElseThrow(() -> new RuntimeException("Organization Not Found with ID: " + id));
        OrganizationDTO wk = mapper.map(organizationRepository.findById(id), OrganizationDTO.class);
        wk.setPlanName("Basic");
        return wk;
    }

    public void changeOrganizationStatus(ChangeAppStatusDTO status, UUID id) {
        Organization organization = organizationRepository.findById(id).orElseThrow(() -> new RuntimeException("Organization Not Found with ID: " + id));
        if (status.getStatus() != null && !status.getStatus().name().isBlank()) {
            organization.setStatus(status.getStatus());
        }
        organizationRepository.save(organization);
    }

    @Transactional
    public Organization createOrganization(CreateOrganizationRequest request) {
        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("Unauthorized. Please log in.");
        }

        if (currentUser.getRole() == UserRole.ATTENDEE) {
            log.warn("SECURITY ALERT: Attendee {} attempted to create an organization.", currentUser.getUsername());
            throw new RuntimeException("Attendees are not permitted to create organizations.");
        }

        List<Organization> conflicts = organizationRepository.findConflicts(
                request.getName(),request.getEmail(), null);

        if (!conflicts.isEmpty()) {
            for (Organization o : conflicts) {
                if (request.getName().equals(o.getName())) {
                    throw new RuntimeException("Organization with Same name already Exists");
                }
                if (request.getEmail().equals(o.getEmail())) {
                    throw new RuntimeException("Organization with Same email already Exists");
                }
            }
        }

        boolean alreadyHasOrg = organizationMemberRepository.existsByUserIdAndRole(currentUser.getId(), OrgRole.OWNER);


        if (alreadyHasOrg) {
            throw new RuntimeException("You already own an organization. Multiple organizations are not allowed in the current plan.");
        }

        Organization newOrg = new Organization();
        newOrg.setName(request.getName());
        newOrg.setEmail(request.getEmail());
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            try {
                String imageUrl = fileService.saveFile(request.getImage(), FileType.IMAGE);
                newOrg.setImageUrl(imageUrl);
            } catch (IOException e) {
                log.error("Failed to save organization picture", e);
            }
        }
        newOrg.setStatus(AppStatus.ACTIVE);
        newOrg.setPendingBalance(BigDecimal.valueOf(0.0));
        newOrg.setSubscriptionStatus(AppStatus.ACTIVE);
        newOrg.setPlanId(UUID.randomUUID());
        newOrg.setSubscriptionEndDate(LocalDateTime.now().plusYears(1)); // Give them 1 free year by default
        newOrg.setCreatedBy(currentUser.getId());
        newOrg.setCreatedAt(LocalDateTime.now());
        newOrg.setModifiedBy(currentUser.getId());
        newOrg.setModifiedAt(LocalDateTime.now());


        // Save to Database
        Organization savedOrg = organizationRepository.save(newOrg);

        // 2. Instantly assign the current user as the "OWNER"
        OrganizationMember ownerMapping = new OrganizationMember();
        ownerMapping.setOrganizationId(savedOrg.getId());
        ownerMapping.setUserId(currentUser.getId());
        ownerMapping.setRole(OrgRole.OWNER);
        ownerMapping.setJoinedAt(LocalDateTime.now());

        organizationMemberRepository.save(ownerMapping);

        log.info("Organization '{}' created successfully by User ID: {}", savedOrg.getName(), currentUser.getId());

        return savedOrg;
    }

    private void verifyOwnerAccess(UUID organizationId, UUID userId) {
        OrganizationMember member = organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this organization."));

        if (!member.getRole().equals(OrgRole.OWNER)) {
            log.warn("SECURITY ALERT: User {} attempted unauthorized edit on Org {}", userId, organizationId);
            throw new RuntimeException("Access Denied: Only the Organization Owner can perform this action.");
        }
    }

    public Optional<Organization> getMyOrganization() {
        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("Unauthorized. Please log in.");
        }

        // Call the explicit JOIN query from your OrganizationRepository
        // This fetches the actual Organization object using the flat UUIDs in one fast database hit
        return organizationRepository.findOrganizationsByUserId(currentUser.getId())
                .stream()
                .findFirst();
    }

    @Transactional
    public Organization updateOrganization(UUID organizationId, UpdateOrganizationRequest request) throws IOException {
        User currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("Unauthorized. Please log in.");
        }
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        verifyOwnerAccess(organizationId, currentUser.getId());


        List<Organization> conflicts = organizationRepository.findConflicts(
                request.getName(), request.getEmail(),organizationId);

        if (!conflicts.isEmpty()) {
            for (Organization o : conflicts) {
                if (request.getEmail() != null && request.getEmail().equals(o.getEmail())) {
                    throw new RuntimeException("Organization with same Email Already Exists");
                }
                if (request.getName() != null && request.getName().equals(o.getName())) {
                    throw new RuntimeException("Organization with same Name Already Exists");
                }
            }
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            org.setName(request.getName());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            org.setEmail(request.getEmail());
        }

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String oldFilePath = org.getImageUrl();
            try {
                // 1. Upload the new file
                String newImageUrl = fileService.saveFile(request.getImage(), FileType.IMAGE);

                // 2. Only update the database IF the upload succeeds
                org.setImageUrl(newImageUrl);

                // 3. Only delete the old file IF the new upload succeeds
                if (oldFilePath != null && !oldFilePath.isBlank()) {
                    try {
                        fileService.deleteFile(oldFilePath);
                    } catch (Exception e) {
                        log.error("Could not delete old pic for {}. File path: {}", org.getName(), oldFilePath, e);
                    }
                }
            } catch (IOException e) {
                log.error("Failed to save organization picture", e);
                throw new RuntimeException("Failed to upload the new image. Please try again.");
            }
        }
        org.setModifiedBy(currentUser.getId());
        org.setModifiedAt(LocalDateTime.now());
        return organizationRepository.save(org);
    }
}
