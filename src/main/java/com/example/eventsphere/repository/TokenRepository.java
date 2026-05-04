package com.example.eventsphere.repository;

import com.example.eventsphere.entity.Token;
import com.example.eventsphere.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {

    // We will search by the HASHED token, never the raw token
    Optional<Token> findByTokenHash(String tokenHash);

    // Cleanly delete all tokens of a specific type for a specific user
    @Modifying
    void deleteByUserIdAndTokenType(UUID userId, TokenType tokenType);

    // 2. THE GARBAGE COLLECTOR: Hard delete tokens whose expiration date is in the past
    @Modifying
    @Query("DELETE FROM Token t WHERE t.expiresAt < :now")
    void deleteAllExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Token t SET t.isRevoked = true WHERE t.userId = :userId AND t.tokenType = :tokenType")
    void revokeAllByUserIdAndTokenType(@Param("userId") UUID userId, @Param("tokenType") TokenType tokenType);

    // Optional: Find an active token for a user
    Optional<Token> findByUserIdAndTokenTypeAndIsRevokedFalse(UUID userId, TokenType tokenType);
}