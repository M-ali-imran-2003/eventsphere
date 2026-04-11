package com.example.eventsphere.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateProfileDTO {

    @Size(max = 30)
    private String name;

    @Email
    private String email;

    private String password; // Optional: only if they want to change it

    private MultipartFile profilePic; // The new image file

    @Size(max = 20)
    private String phoneNo;

}
