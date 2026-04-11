package com.example.eventsphere.dto;

import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserDTO {

    private UUID id;

    private String name;

    private String username;

    private String cnic;

    private String email;

    private String profilePic;

    private String phoneNo;

    private UserRole role;

    private AppStatus Status;

    private LocalDateTime createdAt;

    private UUID createdBy;

    private LocalDateTime modifiedAt;

    private UUID modifiedBy;

}
