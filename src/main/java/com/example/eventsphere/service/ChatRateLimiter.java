package com.example.eventsphere.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Simple fixed-window rate limiter on Redis (you already run Redis).
 * Protects your Gemini bill: the chat endpoints are public.
 *
 * If Redis is down we ALLOW the request (better a few extra messages than a dead chatbot).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRateLimiter {

    private static final int MAX_PER_IP_PER_MINUTE = 20;
    private static final int MAX_PER_SESSION_PER_MINUTE = 8;
    private static final Duration WINDOW = Duration.ofSeconds(60);

    private final StringRedisTemplate redis;

    /** @return true if the request may continue, false if the visitor is sending too much. */
    public boolean allow(HttpServletRequest request, String sessionId) {
        return hit("rl:chat:ip:" + clientIp(request), MAX_PER_IP_PER_MINUTE)
                && hit("rl:chat:session:" + sessionId, MAX_PER_SESSION_PER_MINUTE);
    }

    private boolean hit(String key, int limit) {
        try {
            Long count = redis.opsForValue().increment(key);
            if (count == null) {
                return true;
            }
            if (count == 1L) {
                redis.expire(key, WINDOW);
            }
            return count <= limit;
        } catch (Exception e) {
            log.warn("Rate limiter unavailable, allowing request: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Behind a proxy (Heroku, Render, Nginx) the proxy APPENDS the real client address
     * at the END of X-Forwarded-For. The first value can be faked by the visitor.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
