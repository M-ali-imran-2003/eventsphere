package com.example.eventsphere.filter;

import com.example.eventsphere.entity.User;
import com.example.eventsphere.service.CustomUserDetailsService;
import com.example.eventsphere.utils.JwtUtil;
import com.example.eventsphere.security.TokenBlackList;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlackList tokenBlacklist;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService, TokenBlackList tokenBlacklist) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);
            if (jwt != null) {

                // 1. Check Logout / Blacklist FIRST
                if (tokenBlacklist.isBlacklisted(jwt)) {
                    log.warn("Attempted access with REVOKED/BLACKLISTED token");
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token is invalidated. Please log in again.");
                    return; // Fast, clean, and perfectly accurate.
                }
                jwtUtil.validateToken(jwt);

                // 3. Set Authentication
                String id = jwtUtil.extractUserId(jwt);
                User userDetails = userDetailsService.loadUserById(UUID.fromString(id));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT token intercepted");
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token has expired. Please log in again.");
        } catch (SignatureException | MalformedJwtException ex) {
            log.warn("Invalid JWT signature/format intercepted");
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token.");
        }
          catch (UsernameNotFoundException ex) {
                log.warn("Valid token received, but user no longer exists in database.");
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "User account no longer exists.");
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication processing error.");
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json"); // Official RFC 9457 Content-Type

        String title = (status == 401) ? "Unauthorized" : "Forbidden";
        String instance = response.getHeader("requestURI"); // or request.getRequestURI() if you pass the request down

        // Formatted to match ProblemDetail RFC 9457
        String json = String.format(
                "{\"type\": \"about:blank\", \"title\": \"%s\", \"status\": %d, \"detail\": \"%s\", \"instance\": \"%s\"}",
                title, status, detail, instance
        );

        response.getWriter().write(json);
    }
}