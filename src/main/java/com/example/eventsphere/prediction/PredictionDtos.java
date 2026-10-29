package com.example.eventsphere.prediction;

import java.util.List;

public final class PredictionDtos {
    private PredictionDtos() {}

    public record EventFeatures(
            String eventId, String title, String category, String city,
            int weekday, int month, int hour, int capacity,
            double avgPrice, int sold, int leadDays, String startDate
    ) {}

    public record PredictionRequest(
            List<EventFeatures> history,
            List<EventFeatures> upcoming
    ) {}

    public record EventForecast(
            String eventId, String title, String category, int capacity, int currentSold,
            Integer predictedTickets, Double predictedGrossRevenue, Integer demandScore,
            String demandLevel, String status, String explanation
    ) {}

    // Historical results are ACTUALS, never forecasts.
    public record CompletedEvent(
            String eventId, String title, String category, int capacity,
            int soldTickets, double actualGrossRevenue,
            int actualSellThroughPercent, String status
    ) {}

    public record DashboardResponse(
            String status, String method, int trainingEventCount, String message,
            Double validationMaeTickets,
            List<EventForecast> predictions,
            List<CompletedEvent> completedEvents
    ) {}
}
