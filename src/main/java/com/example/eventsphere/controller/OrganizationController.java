package com.example.eventsphere.controller;

import com.example.eventsphere.dto.*;
import com.example.eventsphere.entity.Organization;
import com.example.eventsphere.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/organization")
@Slf4j
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-all-organization")
    public ResponseEntity<List<OrganizationListDTO>> getAllOrganization() {
        List<OrganizationListDTO> organization = organizationService.getAllOrganization(); // If not found, throws RuntimeException -> GlobalHandler
        return ResponseEntity.ok(organization);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-organization-by-id/{id}")
    public ResponseEntity<OrganizationDTO> getOrganizationById(@PathVariable UUID id) {
        OrganizationDTO organization = organizationService.getOrganizationById(id); // If not found, throws RuntimeException -> GlobalHandler
        return ResponseEntity.ok(organization);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/update-organization-status/{id}")
    public ResponseEntity<OrganizationDTO> updateStatus(@PathVariable UUID id, @Valid @RequestBody ChangeAppStatusDTO status) {
        organizationService.changeOrganizationStatus(status,id); // If not found, throws RuntimeException -> GlobalHandler
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @PostMapping(value="/create-organization",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Organization> createOrganization(@Valid @ModelAttribute CreateOrganizationRequest request) {
        Organization newOrg = organizationService.createOrganization(request);
        return ResponseEntity.ok(newOrg);
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @PatchMapping(value="/update-organization/{id}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Organization> updateOrganization(@PathVariable UUID id, @Valid @ModelAttribute UpdateOrganizationRequest request) throws IOException {
        Organization newOrg = organizationService.updateOrganization(id,request);
        return ResponseEntity.ok(newOrg);
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/my-org")
    public ResponseEntity<Optional<Organization>> getMyOrganization() {
        Optional<Organization> myOrg = organizationService.getMyOrganization();
        return ResponseEntity.ok(myOrg);// If not found, return 404
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/wallet/{organizationId}")
    public ResponseEntity<WalletDashboardResponse> getWalletDashboard(@PathVariable UUID organizationId) {
        WalletDashboardResponse walletData = organizationService.getWalletDashboard(organizationId);
        return ResponseEntity.ok(walletData);
    }


}
