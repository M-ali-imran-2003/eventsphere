package com.example.eventsphere.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

@Data
public class UpdateSubEventRequest {
    private String title;
    private String description;
    private MultipartFile image;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String roomOrLocation;
    private Integer capacityLimit;
    private String rulesConfig;
}