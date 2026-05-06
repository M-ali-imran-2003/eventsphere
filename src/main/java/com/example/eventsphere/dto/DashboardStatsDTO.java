package com.example.eventsphere.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalUsers;
    private long totalOrganization;
    private long totalEvents;
    private long totalCategories;

}