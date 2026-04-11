package com.example.eventsphere.utils;

import com.example.eventsphere.entity.User;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    /**
     * Gets the full User entity from the SecurityContext.
     * Returns null if no user is authenticated or if it's an anonymous user.
     */
    public User getCurrentUser() {
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


    public boolean isAuthenticated() {
        return getCurrentUser() != null;
    }
}
