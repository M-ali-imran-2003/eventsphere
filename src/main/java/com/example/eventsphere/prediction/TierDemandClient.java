package com.example.eventsphere.prediction;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TierDemandClient {
    private static final Logger LOG = LoggerFactory.getLogger(TierDemandClient.class);
    private final RestClient client;
    private final String serviceKey;

    public TierDemandClient(
            @Value("${prediction.service.url:http://127.0.0.1:8001}") String url,
            @Value("${prediction.api.key:}") String serviceKey
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(12000);
        client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
        this.serviceKey = serviceKey;
    }

    public TierDemandDtos.ForecastResponse predict(TierDemandDtos.DemandRequest request) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "PREDICTION_API_KEY not configured in Spring Boot");
        }
        try {
            TierDemandDtos.ForecastResponse response = client.post()
                    .uri("/predict/tier-demand")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Service-Key", serviceKey)
                    .body(request)
                    .retrieve()
                    .body(TierDemandDtos.ForecastResponse.class);
            if (response == null) throw new IllegalStateException("Empty prediction response");
            return response;
        } catch (RestClientResponseException ex) {
            // FastAPI returns HTTP 422 with the precise failing Pydantic field.
            // Log the response body locally, but never log the shared service key.
            String responseBody = ex.getResponseBodyAsString();
            if (responseBody != null && responseBody.length() > 3000) {
                responseBody = responseBody.substring(0, 3000) + "...";
            }
            LOG.warn("Tier forecast FastAPI HTTP {} for event {}: {}",
                    ex.getStatusCode().value(), request.eventId(), responseBody);
            String reason = switch (ex.getStatusCode().value()) {
                case 401 -> "ML service rejected its API key. Check PREDICTION_API_KEY in both applications.";
                case 422 -> "ML rejected one or more forecast inputs (422). See Spring Boot logs for field details.";
                case 503 -> "ML model is unavailable. Train it, then restart Python.";
                default -> "ML service returned HTTP " + ex.getStatusCode().value() + ". Check Spring Boot logs.";
            };
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, reason, ex);
        } catch (RestClientException | IllegalStateException ex) {
            LOG.warn("Tier forecast service unreachable or produced invalid response for event {}: {}",
                    request.eventId(), ex.toString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not reach or decode the ML service. Confirm Python runs on port 8001 and check logs.", ex);
        }
    }
}
