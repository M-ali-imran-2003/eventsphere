package com.example.eventsphere.dto;

import com.example.eventsphere.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserDTO {

    private UserStatus status;
}
