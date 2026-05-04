package com.example.eventsphere.filter;

import com.example.eventsphere.entity.User;
import com.example.eventsphere.utils.SecurityUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,@NotNull HttpServletResponse response, @NotNull FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        try {
            // 1. Extract Data
            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null) {
                ipAddress = request.getRemoteAddr();
            }
            String method = request.getMethod();
            String uri = request.getRequestURI();

            // 2. Extract User (if authenticated by the time this runs, or do it in the JWT Filter)
            User currentUser = SecurityUtil.getCurrentUser();
            String userIdToLog = (currentUser != null) ? currentUser.getId().toString() : "anonymous_id";
            String usernameToLog = (currentUser != null) ? currentUser.getUsername() : "anonymous_user";

            // 3. Put into MDC
            MDC.put("ip", ipAddress);
            MDC.put("userId", userIdToLog);
            MDC.put("userName", usernameToLog);
            MDC.put("method", request.getMethod());
            MDC.put("uri", request.getRequestURI());

            // Log the incoming request
            log.info("Incoming Request");

            // Continue to your controllers
            filterChain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("Completed in {} ms with status {}", duration, response.getStatus());
            MDC.clear();
        }
    }
}