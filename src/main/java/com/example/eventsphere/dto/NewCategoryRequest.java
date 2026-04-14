package com.example.eventsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewCategoryRequest {
    @NotBlank(message = "Category name cannot be empty")
    @Size(min = 3, max = 20, message = "Size Must be Between 5-50 characters")
    private String name;
}
