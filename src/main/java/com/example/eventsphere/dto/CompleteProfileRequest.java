package com.example.eventsphere.dto;

import com.example.eventsphere.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompleteProfileRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max=15, message = "Password must be at least 8 characters and max 15 characters")
    private String password;

    @NotBlank(message = "CNIC is strictly required to register an account")
    @Size(min=13,max = 13, message = "CNIC must be exact 13 characters")
    private String cnic;

    @NotBlank(message = "Phone number is required")
    @Size(min=11,max = 11, message="Phone Number should be exactly 11")
    private String phoneNo;

    @NotNull(message = "Role is required")
    private UserRole role; // Will expect "ATTENDEE" or "ORGANIZER" from frontend
}