package com.example.eventsphere.service;

import com.example.eventsphere.dto.CompleteProfileRequest;
import com.example.eventsphere.dto.SignupRequest;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.UserRole;
import com.example.eventsphere.enums.UserStatus;
import com.example.eventsphere.mapper.GenericMapper;
import com.example.eventsphere.repository.UserRepository;
import com.example.eventsphere.security.TokenBlackList;
import com.example.eventsphere.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // 1. Create the "Fake" Tools (Mocks)
    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenBlackList tokenBlackList;
    @Mock private OtpService otpService;
    @Mock private EmailService emailService;
    @Mock private GenericMapper mapper;

    // 2. Inject the fake tools into the real AuthService
    @InjectMocks
    private AuthService authService;

    // ==========================================
    // TEST 1: Initiate Signup (Happy Path)
    // ==========================================
    @Test
    void initiateSignup_ShouldSendOtp_WhenEmailIsNew() {
        // GIVEN: A new user request
        SignupRequest request = new SignupRequest();
        request.setEmail("newuser@eventsphere.com");

        // Tell the fake database: "Say this email doesn't exist yet"
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        // Tell the fake OTP service: "Return 123456 when asked"
        when(otpService.generateAndSaveOtp(request.getEmail())).thenReturn("123456");

        // WHEN: We call the actual method
        authService.initiateSignup(request);

        // THEN: Verify the email service was actually triggered exactly 1 time
        verify(emailService, times(1)).sendOtpEmail("newuser@eventsphere.com", "123456");
    }

    // ==========================================
    // TEST 2: Initiate Signup (Edge Case)
    // ==========================================
    @Test
    void initiateSignup_ShouldThrowException_WhenEmailAlreadyExists() {
        // GIVEN: A request for an email that is already in the database
        SignupRequest request = new SignupRequest();
        request.setEmail("existing@eventsphere.com");

        User activeUser = new User();
        activeUser.setStatus(UserStatus.ACTIVE);

        // Tell the fake DB: "Pretend we found an active user!"
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(activeUser));

        // WHEN & THEN: Expect a RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.initiateSignup(request);
        });

        assertEquals("Email is already registered.", exception.getMessage());
        // Verify we NEVER sent an email
        verify(emailService, never()).sendOtpEmail(anyString(), anyString());
    }

    // ==========================================
    // TEST 3: Login Security (Edge Case)
    // ==========================================
    @Test
    void loginUser_ShouldBlockAdminFromStandardPortal() {
        // GIVEN: An admin trying to log into the normal user route
        User adminUser = new User();
        adminUser.setRole(UserRole.ADMIN);

        when(userRepository.findByUsernameOrEmail("admin_hacker", "admin_hacker"))
                .thenReturn(Optional.of(adminUser));

        // WHEN & THEN
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.loginUser("admin_hacker", "password123");
        });

        assertEquals("Access Denied: Admin Not Allowed.", exception.getMessage());
    }

    // ==========================================
    // TEST 4: Complete Profile Security (Edge Case)
    // ==========================================
    @Test
    void completeSignup_ShouldBlockAdminRegistration() {
        // GIVEN: A valid JWT, but a malicious body trying to set role to ADMIN
        String dummyToken = "valid.jwt.token";
        String email = "hacker@eventsphere.com";

        CompleteProfileRequest request = new CompleteProfileRequest();
        request.setRole(UserRole.ADMIN);

        // Mock the JWT extraction
        when(jwtUtil.extractEmail(dummyToken)).thenReturn(email);

        // WHEN & THEN
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.completeSignup(request, dummyToken);
        });

        assertEquals("You do not have permission to register as an Admin.", exception.getMessage());
    }
}