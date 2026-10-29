package com.example.eventsphere.prediction;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class PredictionClient {
    private final RestClient restClient;
    private final String apiKey;

    public PredictionClient(
            @Value("${prediction.service.url:http://localhost:8001}") String baseUrl,
            @Value("${prediction.api.key:}") String apiKey
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(8000);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
        this.apiKey = apiKey;
    }

    public PredictionDtos.DashboardResponse forecast(PredictionDtos.PredictionRequest request) {
        if (apiKey.isBlank()) {
            return unavailable();
        }
        try {
            PredictionDtos.DashboardResponse response = restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Service-Key", apiKey)
                    .body(request)
                    .retrieve()
                    .body(PredictionDtos.DashboardResponse.class);
            return response != null ? response : unavailable();
        } catch (RestClientException ex) {
            // Don't leak internal service URLs or credentials to dashboard users.
            return unavailable();
        }
    }

    private static PredictionDtos.DashboardResponse unavailable() {
        return new PredictionDtos.DashboardResponse(
                "SERVICE_UNAVAILABLE", "NONE", 0,
                "Prediction service is unavailable. Check its URL, service key, and logs.",
                null, List.of(), List.of()
        );
    }
}
