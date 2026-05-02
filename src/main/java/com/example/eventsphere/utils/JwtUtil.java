package com.example.eventsphere.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final long LONG_EXPIRY;
    private final long shortExpiry;
    private final String secretKey;
    private final long regExpiry;
    public JwtUtil(@Value("${jwt.secret}") String secretKey,
                   @Value("${jwt.short.expiration}") long shortExpiry,
                   @Value("${jwt.refresh.expiration}") long LONG_EXPIRY, @Value("${jwt.reg.expiration}") long regExpiry) {
        this.secretKey = secretKey;
        this.shortExpiry = shortExpiry * 1000 * 60;
        this.LONG_EXPIRY = LONG_EXPIRY;
        this.regExpiry = regExpiry * 1000 * 60;
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public void validateToken(String token) {
        extractAllClaims(token);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    public String generateRefreshToken(UUID id) {
        return Jwts.builder()
                .setSubject(id.toString())
                .setIssuedAt(new Date())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String generateToken(UUID id, String role) {
        return Jwts.builder()
                .setSubject(id.toString())
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + shortExpiry))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRegistrationToken(String email) {
        return Jwts.builder()
                .setSubject(email) // The email is the main identity here
                .claim("type", "PRE_REGISTER") // Helps us identify this token later
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + regExpiry))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public long getRemainingTimeInMilliseconds(String token) {
        // Find out exactly when the token is supposed to die
        Date expirationDate = extractExpiration(token);

        // Calculate the difference between that time and right now
        long timeLeft = expirationDate.getTime() - System.currentTimeMillis();

        // If the token is already expired, return 0 instead of a negative number
        return Math.max(timeLeft, 0);
    }


}
