package com.example.eventsphere.dto;

import com.example.eventsphere.enums.AppStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateCategoryDTO {

    @Size(min=3,max=15,message = "Size Must be Between 5-50 characters")
    public String name;

    public AppStatus status;
}
