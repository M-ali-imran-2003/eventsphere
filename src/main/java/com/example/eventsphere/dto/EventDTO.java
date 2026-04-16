package com.example.eventsphere.dto;

import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.utils.LocationUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class EventDTO {

    private UUID id;
    private String workspaceName; // Fixed from UUID to String
    private String categoryName;  // Fixed from UUID to String
    private String title;
    private String address;
    private String city;
    private String state;
    private String country;
    private Double lat;
    private Double lon;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private AppStatus status;
    private LocalDateTime createdAt;
    private UUID createdBy;
    private LocalDateTime modifiedAt;
    private UUID modifiedBy;

    // Custom JPQL Constructor
    public EventDTO(UUID id, String workspaceName, String categoryName, String title,
                    String address, String city, String state, String country,
                    Point location, // Accept the raw JTS Point from the query
                    LocalDateTime startDateTime, LocalDateTime endDateTime, AppStatus status,
                    LocalDateTime createdAt, UUID createdBy, LocalDateTime modifiedAt, UUID modifiedBy) {

        this.id = id;
        this.workspaceName = workspaceName;
        this.categoryName = categoryName;
        this.title = title;
        this.address = address;
        this.city = city;
        this.state = state;
        this.country = country;

        // Convert the geometry Point to Lat/Lon immediately upon creation!
        this.lat = LocationUtil.getLat(location);
        this.lon = LocationUtil.getLon(location);

        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.modifiedAt = modifiedAt;
        this.modifiedBy = modifiedBy;
    }
}