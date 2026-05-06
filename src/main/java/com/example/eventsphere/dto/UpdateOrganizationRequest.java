package com.example.eventsphere.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateOrganizationRequest {

    @Size(min=5,max = 50,message = "Name must be in 5-50 characters")
    private String name;

    @Email
    @Size(max = 30, message = "Email must be under 30 characters")
    private String email;

    private MultipartFile image; // The new image file

}
