package com.example.eventsphere.dto;

import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UsersDTO {

    private UUID id;

    private String name;

    private String username;

    private String email;

    private String profilePic;

    private UserRole role;

    private AppStatus Status;

}
