package com.example.eventsphere.controller;

import com.example.eventsphere.dto.LoginRequest;
import com.example.eventsphere.dto.LoginResponse;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.repository.UserRepository;
import com.example.eventsphere.security.TokenBlackList;
import com.example.eventsphere.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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

}
