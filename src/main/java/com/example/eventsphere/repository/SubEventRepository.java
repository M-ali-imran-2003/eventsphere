package com.example.eventsphere.repository;

import com.example.eventsphere.entity.SubEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubEventRepository extends JpaRepository<SubEvent,UUID> {

    List<SubEvent> findByEventIdOrderByStartTimeAsc(UUID eventId);

}
