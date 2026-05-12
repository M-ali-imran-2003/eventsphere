package com.example.eventsphere.repository;

import com.example.eventsphere.entity.SubEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubEventRepository extends JpaRepository<SubEvent,UUID> {

    List<SubEvent> findByEventIdOrderByStartTimeAsc(UUID eventId);

}
