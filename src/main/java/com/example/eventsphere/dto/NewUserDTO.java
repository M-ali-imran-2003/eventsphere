package com.example.eventsphere.dto;

import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.UserRole;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class NewUserDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 30, message = "Name must be under 30 characters")
    private String name;

    @NotBlank(message = "Username is required")
    @Size(max = 30, message = "Username must be under 30 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min=5,max=15, message = "Password should be between 5-15 characters")
    private String password;

    @NotBlank(message = "CNIC is required")
    @Size(max = 13, message = "CNIC must be under 13 characters")
    private String cnic;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email")
    @Size(max = 30, message = "Email must be under 30 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 13, message="Phone Number should be less than 13")
    private String phoneNo;

    private MultipartFile profilePic;
    private UserRole role;

}
