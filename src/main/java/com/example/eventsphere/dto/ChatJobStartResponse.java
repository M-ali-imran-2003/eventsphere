package com.example.eventsphere.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record ChatJobStartResponse(

        @JsonProperty("job_id")
        UUID jobId,

        String status
) {
}