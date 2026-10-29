package com.example.eventsphere.prediction;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Pure functions for historical performance; no DB or ML dependencies. */
public final class TierHistoricalMetrics {
    private TierHistoricalMetrics() {}

    public static int countFinalDays(Map<LocalDate, Integer> daily, LocalDate eventDate, int windowDays) {
        LocalDate first = eventDate.minusDays(windowDays - 1L);
        return daily.entrySet().stream()
                .filter(e -> !e.getKey().isBefore(first) && !e.getKey().isAfter(eventDate))
                .mapToInt(Map.Entry::getValue).sum();
    }

    public static double percent(int sold, int capacity) {
        return capacity <= 0 ? 0.0 : Math.round((sold * 10000.0) / capacity) / 100.0;
    }

    public static LocalDate firstDay(Map<LocalDate, Integer> daily) {
        return daily.keySet().stream().min(Comparator.naturalOrder()).orElse(null);
    }

    public static LocalDate lastDay(Map<LocalDate, Integer> daily) {
        return daily.keySet().stream().max(Comparator.naturalOrder()).orElse(null);
    }

    /** Earliest date wins when multiple dates tie. */
    public static LocalDate peakDay(Map<LocalDate, Integer> daily) {
        return daily.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .max(Comparator.<Map.Entry<LocalDate, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(e -> e.getKey(), Comparator.reverseOrder()))
                .map(Map.Entry::getKey).orElse(null);
    }

    /** Daily cumulative points (including zero-sale days). Max 365 points, no future projection.
     * Any older sales remain included in the first displayed cumulative count. */
    public static List<TierDemandDtos.SalesDay> chart(Map<LocalDate, Integer> daily,
                                                       LocalDate start, LocalDate through) {
        LocalDate end = through == null ? LocalDate.now() : through;
        LocalDate beginning = start == null ? end : start;
        if (beginning.isAfter(end)) beginning = end;
        LocalDate visibleStart = beginning.isAfter(end.minusDays(364)) ? beginning : end.minusDays(364);
        int previous = daily.entrySet().stream()
                .filter(e -> e.getKey().isBefore(visibleStart))
                .mapToInt(Map.Entry::getValue).sum();
        int size = (int) ChronoUnit.DAYS.between(visibleStart, end) + 1;
        List<TierDemandDtos.SalesDay> points = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            LocalDate day = visibleStart.plusDays(i);
            previous += daily.getOrDefault(day, 0);
            points.add(new TierDemandDtos.SalesDay(day, previous));
        }
        return points;
    }

    public static String insight(int sold, int capacity, int final7, String phase) {
        if (sold == 0) return "No confirmed ticket sales were recorded for this tier.";
        if (capacity <= 0) return "No positive ticket capacity is configured; sell-through cannot be measured.";
        if (sold >= capacity) return "This tier reached its current capacity. Review its pricing and allocation for a future event.";
        double fraction = (double) sold / capacity;
        if (fraction >= .80) return "Strong historical sell-through. Consider a similar capacity for future events.";
        if (fraction < .35) return "Low historical sell-through. Review marketing, ticket pricing and timing for future events.";
        if ("IN_PROGRESS".equals(phase)) return "The event is underway. These are actual sales so far, not a final outcome.";
        if (final7 >= Math.max(3, sold / 3)) return "A substantial share of tickets sold in the final 7 days before the event started.";
        return "Moderate historical sell-through. Compare this tier with other events before changing pricing.";
    }
}
