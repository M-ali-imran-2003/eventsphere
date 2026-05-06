package com.example.eventsphere.entity;

import com.example.eventsphere.enums.OrgRole;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organization_members")
@Data
public class OrganizationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="org_member_id")
    private UUID id;

    @Column(name = "organization_id")
    private UUID organizationId;

    @JoinColumn(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_role")
    private OrgRole role; // "OWNER", "MANAGER", "STAFF"

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

}