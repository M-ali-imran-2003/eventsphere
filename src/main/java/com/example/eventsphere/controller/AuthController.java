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
    public ResponseEntity<String> completeSignup(
            @Valid @RequestBody CompleteProfileRequest request,
            @RequestHeader("Authorization") String authHeader) {

        // 1. Ensure the header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(null);
        }

        // 2. Extract just the token part
        String token = authHeader.substring(7);

        // 3. Complete the signup and get the final login response
        authService.completeSignup(request, token);
        return ResponseEntity.ok("Signup completed Successfully");
    }

    @PostMapping("/forgot-password/initiate")
    public ResponseEntity<String> initiateForgotPassword(@Valid @RequestBody ForgotPasswordInitiateRequest request) {
        authService.initiateForgotPassword(request);
        return ResponseEntity.ok("OTP has been sent");
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<String> verifyForgotPasswordOtp(@Valid @RequestBody ForgotPasswordVerifyRequest request) {
        // Returns the temporary token needed for the final step
        String resetToken = authService.verifyForgotPasswordOtp(request);
        return ResponseEntity.ok(resetToken);
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ForgotPasswordResetRequest request,
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("Error", "Missing or invalid Authorization header."));
        }

        String token = authHeader.substring(7);
        authService.resetPassword(request, token);

        return ResponseEntity.ok(Map.of(
                "Message", "Password has been reset successfully. You can now log in.",
                "Status", "Success"
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshTokens(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

}
