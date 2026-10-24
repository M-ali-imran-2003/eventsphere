package com.example.eventsphere.repository;

import com.example.eventsphere.entity.ChatMessages;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessagesRepository extends JpaRepository<ChatMessages, UUID> {
    List<ChatMessages> findBySessionId(UUID sessionId);

}
