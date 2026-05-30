package com.example.eventsphere.repository;

import com.example.eventsphere.entity.DiscountCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscountCodeRepository extends JpaRepository<DiscountCode, UUID> {
   List<DiscountCode> findByEventId(UUID eventId);
   boolean existsByCodeAndEventId(String code, UUID eventId); // Prevents duplicate codes!
   Optional<DiscountCode> findByCodeAndEventId(String code, UUID eventId);
}
