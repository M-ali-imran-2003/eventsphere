//package com.example.eventsphere.repository;
//
//import com.example.eventsphere.entity.ChatMessages;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.UUID;
//
//public interface ChatMessagesRepository extends JpaRepository<ChatMessages, UUID> {
//    List<ChatMessages> findBySessionId(UUID sessionId);
//
//}
package com.example.eventsphere.repository;

import com.example.eventsphere.entity.ChatMessages;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessagesRepository extends JpaRepository<ChatMessages, UUID> {

    // Ordered: the old findBySessionId had no ORDER BY, so messages could come back shuffled.
    List<ChatMessages>findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
