package com.example.eventsphere.repository;

import com.example.eventsphere.entity.SubEventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SubEventRegistrationRepository extends JpaRepository<SubEventRegistration, UUID> {
}
