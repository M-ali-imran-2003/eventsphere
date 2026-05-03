package com.example.eventsphere.controller;

import com.example.eventsphere.dto.ChangeUserStatusDTO;
import com.example.eventsphere.dto.UserDTO;
import com.example.eventsphere.dto.UserListDTO;
import com.example.eventsphere.service.UserService;
import com.example.eventsphere.filter.JwtAuthenticationFilter;
import com.example.eventsphere.utils.JwtUtil;
import com.example.eventsphere.filter.RequestLoggingFilter;
import com.example.eventsphere.utils.SecurityUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserService userService;

    // === MOCKS TO PREVENT SECURITY FILTER CRASHES ===
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private RequestLoggingFilter requestLoggingFilter;
    @MockitoBean private SecurityUtil securityUtil;
    // ================================================

    @Test
    void getAllUsers_ShouldReturn200AndList() throws Exception {
        when(userService.findAllUsers()).thenReturn(Collections.singletonList(new UserListDTO()));

        mockMvc.perform(get("/api/users/get-all-users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getUserById_ShouldReturn200AndUser() throws Exception {
        UUID testId = UUID.randomUUID();
        when(userService.findById(testId)).thenReturn(new UserDTO());

        mockMvc.perform(get("/api/users/get-user-by-id/" + testId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void updateUserStatus_ShouldReturn200Ok() throws Exception {
        UUID testId = UUID.randomUUID();
        ChangeUserStatusDTO statusDTO = new ChangeUserStatusDTO();

        doNothing().when(userService).updateUserStatus(any(), any());

        mockMvc.perform(patch("/api/users/update-user-status/" + testId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void addAdmin_ShouldReturn200Ok_WhenMultipartFormDataIsSent() throws Exception {
        // Create a fake image to upload via the API
        MockMultipartFile profilePic = new MockMultipartFile(
                "profilePic", "test.png", MediaType.IMAGE_PNG_VALUE, "fake-image-data".getBytes()
        );

        doNothing().when(userService).addAdmin(any());

        // Use the special multipart() builder and include ALL required fields!
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/users/add-admin")
                        .file(profilePic)
                        .param("name", "Ali Imran")
                        .param("cnic", "4210112345671")              // FIX: Removed dashes, now exactly 13 characters!
                        .param("phoneNo", "0300-1234567")
                        .param("username", "newAdmin")
                        .param("email", "admin@eventsphere.com")
                        .param("password", "securePass123")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }
}