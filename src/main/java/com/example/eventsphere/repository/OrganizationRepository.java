package com.example.eventsphere.repository;

import com.example.eventsphere.entity.Organization;
import com.example.eventsphere.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    boolean existsByName(String name);
    boolean existsByEmail(String email);

    @Query("SELECT o FROM Organization o JOIN OrganizationMember m ON o.id = m.organizationId WHERE m.userId = :userId")
    List<Organization> findOrganizationsByUserId(@Param("userId") UUID userId);

    @Query("SELECT o FROM Organization o WHERE (" +
            "(:name IS NULL OR o.name = :name) OR " +
            "(:email IS NULL OR o.email = :email)" +
            ") AND (:id IS NULL OR o.id <> :id)")
    List<Organization> findConflicts(@Param("name")String name,@Param("email") String email,@Param("id")UUID id);
}

