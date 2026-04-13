package com.example.eventsphere.security;

import com.example.eventsphere.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlackList {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    // This puts the token in Redis
    public void add(String token) {
        // Calculate how long until the JWT expires naturally
        long timeLeft = jwtUtil.getRemainingTimeInMilliseconds(token);

        if (timeLeft > 0) {
            // Store in Redis: Key = token, Value = "blacklisted"
            // It will automatically disappear after 'timeLeft' milliseconds
            redisTemplate.opsForValue().set(token, "blacklisted", timeLeft, TimeUnit.MILLISECONDS);
            log.info("Token added to Redis blacklist. Expiring in {}ms", timeLeft);
        }
    }

    // This checks if the token is currently blocked
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(token));
    }
}