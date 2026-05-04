package com.example.eventsphere.utils;

import com.example.eventsphere.entity.User;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SecurityUtil {
    private  SecurityUtil() {}
    /**
     * Gets the full User entity from the SecurityContext.
     * Returns null if no user is authenticated or if it's an anonymous user.
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null &&
                authentication.isAuthenticated() &&
                !(authentication instanceof AnonymousAuthenticationToken)) {

            Object principal = authentication.getPrincipal();

            if (principal instanceof User) {
                return (User) principal;
            }
        }
        return null;
    }


    public static boolean isAuthenticated() {
        return getCurrentUser() != null;
    }

    public static String hashSHA256(String rawString) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawString.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash string securely", e);
        }
    }
}
