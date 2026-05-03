package com.example.eventsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgotPasswordResetRequest {
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 15, message = "Password must be between 8 and 50 characters")
    private String newPassword;

    // ADD THIS NEW FIELD
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}