package com.example.eventsphere.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CreateOrganizationRequest {

    @NotBlank(message = "Organization name is required")
    @Size(min=5,max = 50,message = "Name must be in 5-50 characters")
    private String name;

    @NotBlank(message = "Organization name is required")
    @Email(message = "Please provide a valid email format")
    private String email;

    @NotNull(message = "File is required")
    private MultipartFile image;

}
