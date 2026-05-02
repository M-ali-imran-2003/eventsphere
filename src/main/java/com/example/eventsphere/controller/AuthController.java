package com.example.eventsphere.controller;

import com.example.eventsphere.dto.*;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.repository.UserRepository;
import com.example.eventsphere.security.TokenBlackList;
import com.example.eventsphere.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // No Try-Catch! If it fails, GlobalExceptionHandler returns the 400 Bad Request.
        return ResponseEntity.ok(authService.loginUser(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<LoginResponse> adminLogin(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginAdmin(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(Map.of("Message", "Successfully logged out.", "Status", "Success"));
    }

    @PostMapping("/signup/initiate")
    public ResponseEntity<String> initiateSignup(@Valid @RequestBody SignupRequest request) {
        authService.initiateSignup(request);
        return ResponseEntity.ok("Verification code sent to your email.");
    }

    /**
     * ACT 2: The user enters the 6-digit code they got in their email.
     * Postman route: POST http://localhost:8080/api/auth/signup/verify
     */
    @PostMapping("/signup/verify")
    public ResponseEntity<String> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        // This returns the 15-minute "Golden Ticket" (Registration JWT)
        String registrationToken = authService.verifyOtp(request);
        return ResponseEntity.ok(registrationToken);
    }

    /**
     * ACT 3: The user fills out their password, name, CNIC, etc.
     * Postman route: POST http://localhost:8080/api/auth/signup/complete
     */
    @PostMapping("/signup/complete")
    public ResponseEntity<LoginResponse> completeSignup(
            @Valid @RequestBody CompleteProfileRequest request,
            @RequestHeader("Authorization") String authHeader) {

        // 1. Ensure the header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(null);
        }

        // 2. Extract just the token part
        String token = authHeader.substring(7);

        // 3. Complete the signup and get the final login response
        LoginResponse response = authService.completeSignup(request, token);
        return ResponseEntity.ok(response);
    }

}
