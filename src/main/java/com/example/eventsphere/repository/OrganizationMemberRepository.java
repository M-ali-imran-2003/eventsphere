package com.example.eventsphere.repository;

import com.example.eventsphere.entity.OrganizationMember;
import com.example.eventsphere.enums.OrgRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {
    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    // Checks if the user belongs to ANY org (Used to block duplicate creations)
    boolean existsByUserIdAndRole(UUID userId, OrgRole role);
}
