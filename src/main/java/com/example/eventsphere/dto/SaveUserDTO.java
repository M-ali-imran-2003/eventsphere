package com.example.eventsphere.dto;

import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SaveUserDTO {

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
