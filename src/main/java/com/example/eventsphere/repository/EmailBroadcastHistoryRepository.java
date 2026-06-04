package com.example.eventsphere.repository;

import com.example.eventsphere.entity.EmailBroadcastHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailBroadcastHistoryRepository extends JpaRepository<EmailBroadcastHistory, UUID> {
    // To display the history log on the Organizer Dashboard, newest first
    List<EmailBroadcastHistory> findByEventIdOrderBySentAtDesc(UUID eventId);
}