package com.example.eventsphere.entity;

import com.example.eventsphere.enums.TokenType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_tokens")
@Data
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "token_id")
    private UUID tokenId;

    // Notice: No @OneToOne join! Just a fast, clean UUID.
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "token_hash")
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type")
    private TokenType tokenType;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_revoked")
    private Boolean isRevoked;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isRevoked == null) {
            this.isRevoked = false;
        }
    }
}