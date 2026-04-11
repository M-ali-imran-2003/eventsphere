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
    private final long SHORT_EXPIRY;
    private final String SECRET_KEY;
    public JwtUtil(@Value("${jwt.secret}") String SECRET_KEY,
                   @Value("${jwt.short.expiration}") long SHORT_EXPIRY,
                   @Value("${jwt.refresh.expiration}") long LONG_EXPIRY) {
        this.SECRET_KEY = SECRET_KEY;
        this.SHORT_EXPIRY = SHORT_EXPIRY;
        this.LONG_EXPIRY = LONG_EXPIRY;
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
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
                .setExpiration(new Date(System.currentTimeMillis() + SHORT_EXPIRY))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }


}
