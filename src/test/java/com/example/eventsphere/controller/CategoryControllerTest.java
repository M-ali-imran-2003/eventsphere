package com.example.eventsphere.controller;

import com.example.eventsphere.dto.CategoryDTO;
import com.example.eventsphere.entity.Category;
import com.example.eventsphere.service.CategoryService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CategoryService categoryService;

    // === MOCKS TO PREVENT SECURITY FILTER CRASHES ===
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private RequestLoggingFilter requestLoggingFilter;
    @MockitoBean private SecurityUtil securityUtil;
    // ================================================

    @Test
    void getAllCategories_ShouldReturn200AndList() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(Collections.singletonList(new CategoryDTO()));

        mockMvc.perform(get("/api/categories/get-all-categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getCategoryById_ShouldReturn200AndCategory() throws Exception {
        UUID testId = UUID.randomUUID();
        when(categoryService.findCategory(testId)).thenReturn(new Category());

        mockMvc.perform(get("/api/categories/get-category-by-id/" + testId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void addCategory_ShouldReturn200Ok() throws Exception {
        String jsonPayload = "{\"name\":\"Music Festival\"}";

        doNothing().when(categoryService).addCategory(any());

        mockMvc.perform(post("/api/categories/add-category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk());
    }

    @Test
    void updateCategory_ShouldReturn200Ok() throws Exception {
        UUID testId = UUID.randomUUID();

        // FIX: Changed "Updated Festival" (16 chars) to "Tech Fest" (9 chars) to pass your @Size validation!
        String jsonPayload = "{\"name\":\"Tech Fest\", \"status\":\"ACTIVE\"}";

        doNothing().when(categoryService).updateCategory(eq(testId), any());

        mockMvc.perform(patch("/api/categories/update-category/" + testId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk());
    }
}