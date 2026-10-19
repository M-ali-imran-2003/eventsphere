package com.example.eventsphere.repository;

import com.example.eventsphere.entity.ChatbotJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatbotJobRepository
        extends JpaRepository<ChatbotJob, UUID> {
}