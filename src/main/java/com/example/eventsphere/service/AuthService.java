package com.example.eventsphere.service;

import com.example.eventsphere.dto.LoginResponse;
import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.UserRole;
import com.example.eventsphere.repository.UserRepository;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.security.TokenBlackList;
import com.example.eventsphere.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlackList tokenBlackList;

    public AuthService(UserRepository userRepository,
                       JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder, TokenBlackList tokenBlackList) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.tokenBlackList = tokenBlackList;
    }

    public LoginResponse loginUser(String identifier, String password) {
        User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Invalid Credentials"));

        if (user.getRole().equals(UserRole.ADMIN)) {
            log.warn("Failed login attempt: Admin tried to login via standard user portal. Identifier: {}", identifier);
            throw new RuntimeException("Access Denied: Admin Not Allowed.");
        }
        return verifyAndGenerateResponse(user, password);
    }

    public LoginResponse loginAdmin(String identifier, String password) {
        User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Invalid Credentials"));

        // THE GATEKEEPER: Block token generation if not an admin
        if (!user.getRole().equals(UserRole.ADMIN)) {
            log.warn("Failed login attempt: Non-admin tried to login via admin portal. Identifier: {}", identifier);
            throw new RuntimeException("Access Denied: Admin privileges required.");
        }

        return verifyAndGenerateResponse(user, password);
    }

    private LoginResponse verifyAndGenerateResponse(User user, String password) {
        if (!user.getStatus().equals(AppStatus.ACTIVE)) {
            throw new RuntimeException("Account is inactive or suspended.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        log.info("Successful login for user: {}", user.getUsername());
        return new LoginResponse(
                token,
                user.getRole().name()
        );
    }

    public void logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            tokenBlackList.add(jwt);
            log.info("User successfully logged out and token blacklisted.");
            return;
        }
        throw new RuntimeException("No valid token provided for logout."); // Triggers Global Handler!
    }


}
