package com.example.eventsphere.dto;

import com.example.eventsphere.enums.AppStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class CategoryDTO {
    public UUID id;
    public String name;
    public String code;
    public AppStatus status;

}
