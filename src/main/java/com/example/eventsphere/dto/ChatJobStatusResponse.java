package com.example.eventsphere.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record ChatJobStatusResponse(

        @JsonProperty("job_id")
        UUID jobId,

        String status,

        String answer,

        @JsonProperty("error_message")
        String errorMessage
) {
}