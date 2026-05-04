package com.example.eventsphere.service;

import com.example.eventsphere.entity.Token;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.TokenType;
import com.example.eventsphere.enums.UserRole;
import com.example.eventsphere.repository.TokenRepository;
import com.example.eventsphere.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class TokenService {

    private final TokenRepository tokenRepository;

    public TokenService(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public String createRefreshToken(User user) {
        tokenRepository.revokeAllByUserIdAndTokenType(user.getId(), TokenType.REFRESH);

        String rawToken = UUID.randomUUID() + "-" + UUID.randomUUID();
        String hashedToken = SecurityUtil.hashSHA256(rawToken);

        LocalDateTime expiryDate;
        if (user.getRole() == UserRole.ADMIN) {
            expiryDate = LocalDateTime.now().plusMinutes(30);
        } else {
            expiryDate = LocalDateTime.now().plusHours(1);
        }

        Token tokenEntity = new Token();
        tokenEntity.setUserId(user.getId());
        tokenEntity.setTokenHash(hashedToken);
        tokenEntity.setTokenType(TokenType.REFRESH);
        tokenEntity.setExpiresAt(expiryDate);
        tokenEntity.setIsRevoked(false);

        tokenRepository.save(tokenEntity);
        log.info("Secure Refresh Token generated and hashed for user ID: {}", user.getId());

        return rawToken; // We return the raw string to send to the user!
    }

    public Token verifyRefreshToken(String rawToken) {
        String hashedToken = SecurityUtil.hashSHA256(rawToken);

        Token token = tokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token. Please log in again."));

        if (token.getIsRevoked()) {
            throw new RuntimeException("Session has been revoked. Please log in again.");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(token);
            throw new RuntimeException("Refresh token has expired. Please log in again.");
        }

        return token;
    }

    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        tokenRepository.revokeAllByUserIdAndTokenType(userId, TokenType.REFRESH);
        log.info("All refresh tokens revoked for user ID: {}", userId);
    }

    @Scheduled(cron = "0 0 3 * * ?") // Runs at 03:00:00 every day
    @Transactional
    public void removeExpiredTokensFromDatabase() {
        log.info("Running scheduled database cleanup: Removing naturally expired tokens...");
        tokenRepository.deleteAllExpiredTokens(LocalDateTime.now());
        log.info("Database cleanup complete.");
    }

}