package com.example.eventsphere.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProfileDTO {

    private String name;

    private String username;

    private String cnic;

    private String email;

    private String profilePic;

    private String phoneNo;

    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;


}
