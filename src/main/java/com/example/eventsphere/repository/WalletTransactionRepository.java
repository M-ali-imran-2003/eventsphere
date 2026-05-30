package com.example.eventsphere.repository;

import com.example.eventsphere.entity.WalletTransactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransactions, UUID> {
    List<WalletTransactions> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
