package com.example.eventsphere.prediction;

import com.example.eventsphere.entity.Event;
import com.example.eventsphere.entity.Organization;
import com.example.eventsphere.entity.TicketTier;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.EventStatus;
import com.example.eventsphere.repository.CategoryRepository;
import com.example.eventsphere.repository.EventRepository;
import com.example.eventsphere.repository.OrganizationMemberRepository;
import com.example.eventsphere.repository.OrganizationRepository;
import com.example.eventsphere.repository.TicketTierRepository;
import com.example.eventsphere.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One endpoint for all usable event phases:
 * UPCOMING:  real as-of sales -> ML forecast
 * LIVE:      real sales to now + ML forecast to event end
 * BACKTEST:  historical actuals + forecast from a prior date (no future sales in features)
 * All data is read-only. Only authorized organization members can see the response.
 */
@Service
@RequiredArgsConstructor
public class TierDemandForecastService {
    private static final Logger LOG = LoggerFactory.getLogger(TierDemandForecastService.class);
    private final EventRepository eventRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final TicketTierRepository tierRepository;
    private final CategoryRepository categoryRepository;
    private final NamedParameterJdbcTemplate jdbc;
    private final TierDemandClient client;

    @Transactional(readOnly = true)
    public TierDemandDtos.ForecastResponse forecast(UUID eventId) {
        User caller = SecurityUtil.getCurrentUser();
        if (caller == null) throw new AccessDeniedException("Authentication required");
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        UUID orgId = event.getOrganizationId();
        Organization org = orgId == null ? null : organizationRepository.findById(orgId).orElse(null);
        if (org == null || org.getStatus() != AppStatus.ACTIVE ||
                memberRepository.findByOrganizationIdAndUserId(orgId, caller.getId()).isEmpty()) {
            throw new AccessDeniedException("You cannot view this event's demand analytics");
        }
        if (event.getStartDateTime() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Set an event start date before requesting demand analytics");
        }
        if (event.getEndDateTime() != null && event.getEndDateTime().isBefore(event.getStartDateTime())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Event end time must be after start time");
        }
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cancelled events cannot be forecast; use historical reports if required");
        }
        LocalDateTime now = LocalDateTime.now(); // Matches existing DB LocalDateTime storage.
        LocalDate today = now.toLocalDate();
        LocalDate startDate = event.getStartDateTime().toLocalDate();
        LocalDateTime effectiveEnd = event.getEndDateTime() != null
                ? event.getEndDateTime() : event.getStartDateTime().plusDays(1);
        boolean started = !event.getStartDateTime().isAfter(now);
        boolean ended = !effectiveEnd.isAfter(now);
        if (!started && event.getStatus() == EventStatus.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Inactive future events cannot be forecast until reactivated");
        }

        List<TicketTier> tiers = tierRepository.findByEventId(eventId).stream()
                .filter(t -> t.getTotalCapacity() != null && t.getTotalCapacity() > 0).toList();
        String phase = !started ? "UPCOMING" : ended ? "COMPLETED" : "IN_PROGRESS";
        LocalDateTime cutoff = ended ? effectiveEnd : now;
        Map<UUID, Map<LocalDate, Integer>> actualCounts = loadSuccessfulTicketCounts(eventId, tiers, cutoff);
        TierDemandDtos.HistoricalSummary actual = started
                ? buildHistorical(eventId, tiers, actualCounts, startDate, cutoff.toLocalDate(), phase, cutoff)
                : null;

        if (tiers.isEmpty()) {
            return new TierDemandDtos.ForecastResponse(eventId.toString(), today, startDate,
                    "NO_TIERS", "This event has no positive-capacity ticket tiers; add a tier to enable forecasts.",
                    null, List.of(), started ? "HISTORICAL" : "FORECAST", actual, null);
        }
        if (tiers.size() > 30) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "At most 30 ticket tiers are supported per forecast request");
        }

        String category = event.getCategoryId() == null ? "Other" : categoryRepository
                .findById(event.getCategoryId()).map(c -> c.getName()).orElse("Other");
        String warning = "Forecast ML is trained on synthetic data and has not been calibrated on real events. " +
                "Ticket sale dates approximate payment time using orders.created_at; capacity and price are CURRENT tier values.";
        if (ended) {
            // Perform a fair historical replay: only orders before the snapshot date go to the ML model.
            // Compare its predicted final sell-through with REAL sales through event end.
            LocalDate replayDate = replayDate(event, tiers);
            LocalDateTime replayCutoff = replayDate.atTime(23, 59, 59);
            Map<UUID, Map<LocalDate, Integer>> pastCounts = loadSuccessfulTicketCounts(eventId, tiers, replayCutoff);
            TierDemandDtos.DemandRequest pastRequest = buildRequest(event, tiers, pastCounts,
                    category, replayDate, startDate);
            try {
                TierDemandDtos.ForecastResponse predicted = client.predict(pastRequest);
                TierDemandDtos.ReplaySummary replay = evaluateReplay(replayDate, predicted.predictions(), actual);
                return new TierDemandDtos.ForecastResponse(eventId.toString(), replayDate, startDate,
                        "RANDOM_FOREST_SYNTHETIC_REPLAY", warning + " Historical replay uses only orders at/before the replay date; " +
                        "the prediction is simulated retrospectively, NOT a saved historical forecast.",
                        predicted.evaluation(), predicted.predictions(), "BACKTEST", actual, replay);
            } catch (ResponseStatusException ex) {
                LOG.warn("Historical replay unavailable for event {}: {}", eventId, ex.getReason());
                return new TierDemandDtos.ForecastResponse(eventId.toString(), today, startDate,
                        "HISTORICAL_ACTUAL", "Actual results are available. Replay could not run: " +
                        "verify that the Python service is running and the model is trained.",
                        null, List.of(), "HISTORICAL", actual, null);
            }
        }

        // Future: forecast until start; live: forecast remaining sales until end.
        LocalDate targetDate = started ? effectiveEnd.toLocalDate() : startDate;
        TierDemandDtos.DemandRequest request = buildRequest(event, tiers, actualCounts,
                category, today, targetDate);
        TierDemandDtos.ForecastResponse predicted = client.predict(request);
        return new TierDemandDtos.ForecastResponse(eventId.toString(), today, targetDate,
                predicted.method(), warning + (started
                ? " Event already started: forecast target is its END date (or start+1 day if no end date is stored)."
                : " Draft forecasts are previews; demand may change once ticket sales open."),
                predicted.evaluation(), predicted.predictions(), started ? "LIVE" : "FORECAST",
                actual, null);
    }

    private static LocalDate replayDate(Event event, List<TicketTier> tiers) {
        LocalDate start = event.getStartDateTime().toLocalDate();
        // Backtest as of 7 days before event start, or halfway through the recorded sale window
        // when publication was more recent. Never include event-day purchases in features.
        LocalDate created = event.getCreatedAt() == null ? start.minusDays(14) : event.getCreatedAt().toLocalDate();
        for (TicketTier tier : tiers) if (tier.getCreatedAt() != null && tier.getCreatedAt().toLocalDate().isBefore(created)) {
            created = tier.getCreatedAt().toLocalDate();
        }
        long window = ChronoUnit.DAYS.between(created, start);
        long back = window > 1 ? Math.min(7, Math.max(1, window / 2)) : 1;
        return start.minusDays(back);
    }

    private TierDemandDtos.DemandRequest buildRequest(Event event, List<TicketTier> tiers,
            Map<UUID, Map<LocalDate, Integer>> counts, String category, LocalDate asOf, LocalDate target) {
        List<TierDemandDtos.TierInput> inputs = new ArrayList<>();
        int daysToEvent = (int) Math.max(0, ChronoUnit.DAYS.between(asOf, target));
        for (TicketTier tier : tiers) {
            Map<LocalDate, Integer> daily = counts.getOrDefault(tier.getId(), Map.of());
            int sold = daily.values().stream().mapToInt(Integer::intValue).sum();
            int capacity = tier.getTotalCapacity();
            if (sold > capacity) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Confirmed tickets exceed current capacity for tier " + tier.getTierName() +
                                "; adjust capacity or reconcile data before forecasting");
            }
            int sold3 = TierHistoricalMetrics.countFinalDays(daily, asOf, 3);
            int sold7 = TierHistoricalMetrics.countFinalDays(daily, asOf, 7);
            LocalDateTime created = event.getCreatedAt() == null ? asOf.atStartOfDay() : event.getCreatedAt();
            if (tier.getCreatedAt() != null && tier.getCreatedAt().isAfter(created)) created = tier.getCreatedAt();
            int elapsed = (int) Math.max(1, ChronoUnit.DAYS.between(created.toLocalDate(), asOf));
            String kind = tier.getTierName() != null && tier.getTierName().toUpperCase().contains("VIP")
                    ? "VIP" : "GENERAL";
            inputs.add(new TierDemandDtos.TierInput(
                    tier.getId().toString(), tier.getTierName(), kind, category,
                    capacity, tier.getPrice() == null ? 0 : tier.getPrice().doubleValue(),
                    sold, sold3, sold7, elapsed, daysToEvent, cumulativeSalesDays(daily, asOf)));
        }
        return new TierDemandDtos.DemandRequest(event.getId().toString(), asOf, target, inputs);
    }

    private static TierDemandDtos.ReplaySummary evaluateReplay(LocalDate replayDay,
            List<TierDemandDtos.TierForecast> predictions, TierDemandDtos.HistoricalSummary actual) {
        Map<String, TierDemandDtos.HistoricalTier> actualByTier = new HashMap<>();
        for (TierDemandDtos.HistoricalTier tier : actual.tiers()) actualByTier.put(tier.tierId(), tier);
        List<TierDemandDtos.ReplayTierResult> results = new ArrayList<>();
        int modelErrors = 0, baselineErrors = 0;
        for (TierDemandDtos.TierForecast pred : predictions) {
            TierDemandDtos.HistoricalTier known = actualByTier.get(pred.tierId());
            if (known == null) continue;
            int mlError = Math.abs(pred.projectedSold() - known.soldTickets());
            int simpleError = Math.abs(pred.naiveProjectedSold() - known.soldTickets());
            results.add(new TierDemandDtos.ReplayTierResult(pred.tierId(), pred.tierName(),
                    pred.projectedSold(), pred.naiveProjectedSold(), known.soldTickets(), mlError, simpleError));
            modelErrors += mlError;
            baselineErrors += simpleError;
        }
        int n = Math.max(1, results.size());
        return new TierDemandDtos.ReplaySummary(replayDay,
                "Historical replay is an illustrative retrospective simulation using a synthetic-trained model. " +
                "Actual totals are measured through event end; not a real prospective accuracy study.",
                Math.round(100.0 * modelErrors / n) / 100.0,
                Math.round(100.0 * baselineErrors / n) / 100.0, results);
    }

    /** CONFIRMED tickets only. Pending/failed orders never count toward ML features. */
    private Map<UUID, Map<LocalDate, Integer>> loadSuccessfulTicketCounts(
            UUID eventId, List<TicketTier> tiers, LocalDateTime cutoff) {
        Map<UUID, Map<LocalDate, Integer>> daily = new HashMap<>();
        if (tiers.isEmpty()) return daily;
        var params = new MapSqlParameterSource()
                .addValue("eventId", eventId)
                .addValue("ids", tiers.stream().map(TicketTier::getId).toList())
                .addValue("cutoff", cutoff);
        jdbc.query("""
                SELECT t.tier_id AS tier_id, CAST(o.created_at AS DATE) AS sale_day,
                       COUNT(*) AS ticket_count
                FROM attendee_tickets t
                JOIN orders o ON o.order_id = t.order_id
                WHERE t.tier_id IN (:ids)
                  AND o.event_id = :eventId
                  AND o.payment_status = 'SUCCESS'
                  AND o.created_at IS NOT NULL
                  AND o.created_at <= :cutoff
                GROUP BY t.tier_id, CAST(o.created_at AS DATE)
                ORDER BY t.tier_id, sale_day
                """, params, rs -> {
            UUID id = rs.getObject("tier_id", UUID.class);
            LocalDate day = rs.getDate("sale_day").toLocalDate();
            daily.computeIfAbsent(id, ignored -> new HashMap<>()).put(day, rs.getInt("ticket_count"));
        });
        return daily;
    }

    private TierDemandDtos.HistoricalSummary buildHistorical(
            UUID eventId, List<TicketTier> tiers, Map<UUID, Map<LocalDate, Integer>> counts,
            LocalDate startDate, LocalDate cutoffDate, String phase, LocalDateTime cutoff) {
        int totalCapacity = 0, totalSold = 0;
        BigDecimal faceValue = BigDecimal.ZERO;
        List<TierDemandDtos.HistoricalTier> results = new ArrayList<>();
        for (TicketTier tier : tiers) {
            Map<LocalDate, Integer> daily = counts.getOrDefault(tier.getId(), Map.of());
            int sold = daily.values().stream().mapToInt(Integer::intValue).sum();
            int capacity = tier.getTotalCapacity();
            BigDecimal price = tier.getPrice() == null ? BigDecimal.ZERO : tier.getPrice();
            BigDecimal tierGross = price.multiply(BigDecimal.valueOf(sold));
            totalCapacity += capacity;
            totalSold += sold;
            faceValue = faceValue.add(tierGross);
            LocalDate peak = TierHistoricalMetrics.peakDay(daily);
            LocalDate first = TierHistoricalMetrics.firstDay(daily);
            LocalDate created = tier.getCreatedAt() == null ? startDate : tier.getCreatedAt().toLocalDate();
            LocalDate chartStart = first == null || created.isBefore(first) ? created : first;
            int final3 = TierHistoricalMetrics.countFinalDays(daily, startDate, 3);
            int final7 = TierHistoricalMetrics.countFinalDays(daily, startDate, 7);
            results.add(new TierDemandDtos.HistoricalTier(
                    tier.getId().toString(), tier.getTierName(), capacity, sold, price, tierGross,
                    TierHistoricalMetrics.percent(sold, capacity), final3, final7,
                    first, TierHistoricalMetrics.lastDay(daily), peak,
                    peak == null ? 0 : daily.get(peak),
                    TierHistoricalMetrics.insight(sold, capacity, final7, phase),
                    TierHistoricalMetrics.chart(daily, chartStart, cutoffDate)));
        }
        BigDecimal paidGross = jdbc.queryForObject("""
                SELECT COALESCE(SUM(o.total_amount), 0)
                FROM orders o
                WHERE o.event_id = :eventId
                  AND o.payment_status = 'SUCCESS'
                  AND o.created_at IS NOT NULL
                  AND o.created_at <= :cutoff
                """, new MapSqlParameterSource().addValue("eventId", eventId).addValue("cutoff", cutoff), BigDecimal.class);
        TierDemandDtos.HistoricalTier top = results.stream()
                .max(Comparator.comparingInt(TierDemandDtos.HistoricalTier::soldTickets)).orElse(null);
        return new TierDemandDtos.HistoricalSummary(phase, cutoffDate, totalCapacity, totalSold,
                TierHistoricalMetrics.percent(totalSold, totalCapacity),
                paidGross == null ? BigDecimal.ZERO : paidGross, faceValue,
                top == null ? null : top.tierId(), top == null ? null : top.tierName(), results);
    }

    /** Keep the current-day point and at most 364 earlier sale dates; 365 FastAPI maximum. */
    static List<TierDemandDtos.SalesDay> cumulativeSalesDays(Map<LocalDate, Integer> daily, LocalDate today) {
        LocalDate oldestVisible = today.minusDays(364);
        int running = daily.entrySet().stream().filter(e -> e.getKey().isBefore(oldestVisible))
                .mapToInt(Map.Entry::getValue).sum();
        List<LocalDate> visible = daily.keySet().stream()
                .filter(d -> !d.isBefore(oldestVisible) && !d.isAfter(today))
                .sorted().toList();
        List<TierDemandDtos.SalesDay> chart = new ArrayList<>(visible.size() + 1);
        for (LocalDate date : visible) {
            running += daily.getOrDefault(date, 0);
            chart.add(new TierDemandDtos.SalesDay(date, running));
        }
        if (chart.isEmpty() || !chart.get(chart.size() - 1).date().equals(today)) {
            chart.add(new TierDemandDtos.SalesDay(today, running));
        }
        return chart;
    }
}
