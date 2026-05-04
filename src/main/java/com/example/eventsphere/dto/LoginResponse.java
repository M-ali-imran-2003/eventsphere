package com.example.eventsphere.dto;

import com.example.eventsphere.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken; // ADD THIS NEW FIELD
    private String role;
}
