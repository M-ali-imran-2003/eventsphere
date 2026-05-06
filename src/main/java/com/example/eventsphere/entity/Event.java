package com.example.eventsphere.entity;


import com.example.eventsphere.enums.AppStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "events")
@Data
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id")
    private UUID id;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "venue_name")
    private String venue;

    @Column(name = "image_url")
    private String image_url;

    @Column(name = "formatted_address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state_region")
    private String state;

    @Column(name = "country")
    private String country;

    @Column(name = "location",columnDefinition = "geography(Point, 4326)")
    private Point location;

    @Column(name = "start_datetime")
    private LocalDateTime startDateTime;

    @Column(name = "end_datetime")
    private LocalDateTime endDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AppStatus status;

    @Column(name = "layout_type")
    private String layoutType;

    @Column(name = "search_tags", columnDefinition = "text[]")
    private String[] tags;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb",name = "type_specific_data")
    private Map<String, Object> typeSpecificData;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by")
    private UUID modifiedBy;
}
