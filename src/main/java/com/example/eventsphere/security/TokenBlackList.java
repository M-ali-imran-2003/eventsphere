package com.example.eventsphere.security;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlackList {
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    public void add(String token) {
        blacklistedTokens.add(token);
    }

    public boolean contains(String token) {
        return blacklistedTokens.contains(token);
    }
}
