package com.example.eventsphere.dto;

import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.utils.LocationUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.util.UUID;

@Data
@NoArgsConstructor
public class EventMapMarkerDTO {

    private UUID id;
    private String title;          // For the hover tooltip
    private String organizationName;  // To show who is hosting it on hover
    private Double lat;
    private Double lon;
    private AppStatus status;      // To color the pin

    // Custom Constructor for JPQL
    public EventMapMarkerDTO(UUID id, String title, String organizationName, Point location, AppStatus status) {
        this.id = id;
        this.title = title;
        this.organizationName = organizationName;
        this.lat = LocationUtil.getLat(location);
        this.lon = LocationUtil.getLon(location);
        this.status = status;
    }
}