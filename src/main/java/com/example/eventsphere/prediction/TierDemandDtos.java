package com.example.eventsphere.prediction;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Shared contracts: Java owns database reads and tenant authorization. */
public final class TierDemandDtos {
    private TierDemandDtos() {}

    // JSON contract with FastAPI: ISO date strings, not Jackson timestamp arrays.
    public record SalesDay(
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate date,
            int sold
    ) {}

    public record TierInput(
            String tierId, String tierName, String tierType, String category,
            int capacity, double price, int sold, int sold3d, int sold7d,
            int daysSincePublish, int daysToEvent, List<SalesDay> actualSales
    ) {}

    public record DemandRequest(
            String eventId,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate asOf,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate eventDate,
            List<TierInput> tiers
    ) {}

    public record TierForecast(
            String tierId, String tierName, int capacity, int sold, double price,
            int sold3d, int sold7d, int projectedSold, int naiveProjectedSold, double projectedSellThrough,
            double sellOutProbability,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate estSellOutDate,
            String status,
            String recommendation, List<SalesDay> actualSales, List<SalesDay> projection
    ) {}

    public record Evaluation(
            String source, int eventGroupsTrain, int eventGroupsHoldout, int holdoutSnapshots,
            double rfSellThroughMAEPercentagePoints,
            double naivePaceMAEPercentagePoints,
            double sellOutProbabilityBrierScore,
            String warning
    ) {}

    /** Historical actuals; valueGross = successful-ticket count * current tier list price.
     * This is NOT cash received if there were discounts or historical price changes. */
    public record HistoricalTier(
            String tierId, String tierName, int capacity, int soldTickets,
            BigDecimal currentTicketPrice, BigDecimal faceValueGross,
            double actualSellThroughPercent, int final3DaysSold, int final7DaysSold,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate firstSaleDate,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate lastSaleDate,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate peakSalesDate,
            int peakDailyTickets, String insight, List<SalesDay> actualSales
    ) {}

    /** recordedOrderGross sums orders.total_amount only for SUCCESS orders of the event.
     * It is not necessarily net revenue after fees or later adjustments. */
    public record HistoricalSummary(
            String eventPhase,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate salesThroughDate,
            int totalCapacity,
            int totalTicketsSold, double overallSellThroughPercent,
            BigDecimal recordedOrderGross, BigDecimal faceValueGross,
            String topTierId, String topTierName,
            List<HistoricalTier> tiers
    ) {}

    /** Historical replay compares a prediction made at a past date against actual final sales.
     * This is a DEMONSTRATION, not proof of out-of-sample real-world accuracy. */
    public record ReplayTierResult(String tierId, String tierName,
                                   int predictedSold, int naiveProjectedSold, int actualSold,
                                   int absoluteModelErrorTickets, int absoluteNaiveErrorTickets) {}

    public record ReplaySummary(
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate simulatedAsOf,
            String note,
                                double modelMeanAbsoluteErrorTickets,
                                double naiveMeanAbsoluteErrorTickets,
                                List<ReplayTierResult> tiers) {}

    /** V6 keeps V4 fields, adds optional replay for completed events. */
    public record ForecastResponse(
            String eventId,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate asOf,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate eventDate,
            String method, String dataWarning, Evaluation evaluation,
            List<TierForecast> predictions,
            String viewType, HistoricalSummary historical, ReplaySummary replay
    ) {
        public ForecastResponse withView(String view, HistoricalSummary summary) {
            return new ForecastResponse(eventId, asOf, eventDate,
                    method, dataWarning, evaluation, predictions, view, summary, replay);
        }
    }
}
