package com.example.eventsphere.dto;

import com.example.eventsphere.enums.UserStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Data
public class ChangeUserStatusDTO {

    private UserStatus status;

}
