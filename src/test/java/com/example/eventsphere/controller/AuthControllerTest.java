package com.example.eventsphere.controller;

import com.example.eventsphere.dto.LoginRequest;
import com.example.eventsphere.dto.LoginResponse;
import com.example.eventsphere.dto.SignupRequest;
import com.example.eventsphere.service.AuthService;
import com.example.eventsphere.filter.JwtAuthenticationFilter;
import com.example.eventsphere.utils.JwtUtil;
// === NEW IMPORTS FOR YOUR LOGGING FILTER ===
import com.example.eventsphere.filter.RequestLoggingFilter;
import com.example.eventsphere.utils.SecurityUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    // === THE JWT MOCKS ===
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    // === THE LOGGING MOCKS (The Fix!) ===
    @MockitoBean
    private RequestLoggingFilter requestLoggingFilter;

    @MockitoBean
    private SecurityUtil securityUtil;
    // =====================================

    @Test
    void initiateSignup_ShouldReturn200Ok() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setEmail("api_test@eventsphere.com");

        doNothing().when(authService).initiateSignup(any(SignupRequest.class));

        mockMvc.perform(post("/api/auth/signup/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Verification code sent to your email."));
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsAreValid() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("ali_imran");
        request.setPassword("securePass123");

        LoginResponse mockResponse = new LoginResponse("mock-jwt-token", "12312","ATTENDEE");

        when(authService.loginUser("ali_imran", "securePass123")).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.role").value("ATTENDEE"));
    }

    @Test
    void logout_ShouldReturnSuccessMessage() throws Exception {
        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer valid.token.here"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Message").value("Successfully logged out."));
    }

    @Test
    void api_ShouldReturn400BadRequest_WhenServiceThrowsRuntimeException() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("hacker");
        request.setPassword("wrongpass");

        when(authService.loginUser("hacker", "wrongpass"))
                .thenThrow(new RuntimeException("Invalid Credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Invalid Credentials"));
    }
}