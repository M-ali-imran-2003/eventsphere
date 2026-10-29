package com.example.eventsphere.prediction;

import com.example.eventsphere.entity.Category;
import com.example.eventsphere.entity.Event;
import com.example.eventsphere.entity.Organization;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.EventStatus;
import com.example.eventsphere.repository.CategoryRepository;
import com.example.eventsphere.repository.OrganizationMemberRepository;
import com.example.eventsphere.repository.OrganizationRepository;
import com.example.eventsphere.utils.SecurityUtil;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Organization-owned published events only. Read-only: does not mutate database. */
@Service
@RequiredArgsConstructor
public class DemandPredictionService {
    private final EntityManager entityManager;
    private final NamedParameterJdbcTemplate jdbc;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final PredictionClient predictionClient;

    private record Stats(int capacity, double avgPrice, int sold, double actualRevenue) {}

    @Transactional(readOnly = true)
    public PredictionDtos.DashboardResponse getDashboard(UUID organizationId) {
        User user = SecurityUtil.getCurrentUser();
        if (user == null) throw new AccessDeniedException("Authentication required");

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        if (org.getStatus() != AppStatus.ACTIVE ||
                memberRepository.findByOrganizationIdAndUserId(organizationId, user.getId()).isEmpty()) {
            throw new AccessDeniedException("You cannot view predictions for this organization");
        }

        // Published/active only: draft tests, cancelled and inactive events are excluded.
        List<Event> publishedEvents = entityManager.createQuery("""
                SELECT e FROM Event e
                WHERE e.organizationId = :organizationId
                  AND e.startDateTime IS NOT NULL
                  AND e.status IN :statuses
                """, Event.class)
                .setParameter("organizationId", organizationId)
                .setParameter("statuses", List.of(EventStatus.PUBLISHED, EventStatus.ACTIVE))
                .getResultList();

        Map<UUID, Stats> stats = loadStats(publishedEvents);
        // IMPORTANT: no ticket tiers / zero capacity => event is completely omitted.
        List<Event> ticketed = publishedEvents.stream()
                .filter(e -> stats.getOrDefault(e.getId(), new Stats(0, 0, 0, 0)).capacity() > 0)
                .toList();
        if (ticketed.isEmpty()) {
            return new PredictionDtos.DashboardResponse(
                    "NO_TICKETED_EVENTS", "NONE", 0,
                    "No published events with ticket capacity found for this organization.",
                    null, List.of(), List.of());
        }

        Map<UUID, String> categories = categoryRepository.findAllById(ticketed.stream()
                .map(Event::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Category::getId, Category::getName));

        LocalDateTime now = LocalDateTime.now();
        List<Event> futureOrOngoing = new ArrayList<>();
        List<Event> completed = new ArrayList<>();
        List<Event> pastMissingEnd = new ArrayList<>();

        for (Event event : ticketed) {
            // Ongoing events can still be forecast to final ticket sales.
            if (!event.getStartDateTime().isBefore(now) ||
                    (event.getEndDateTime() != null && event.getEndDateTime().isAfter(now))) {
                futureOrOngoing.add(event);
            } else if (event.getEndDateTime() != null && !event.getEndDateTime().isAfter(now)) {
                completed.add(event);
            } else {
                // No end date: show recorded performance but do not train on it.
                pastMissingEnd.add(event);
            }
        }

        // Show all past published ticketed events with REAL sales (not invented forecasts).
        List<Event> pastForDisplay = new ArrayList<>(completed);
        pastForDisplay.addAll(pastMissingEnd);
        List<PredictionDtos.CompletedEvent> completedEvents = pastForDisplay.stream()
                .map(e -> {
                    Stats s = stats.get(e.getId());
                    return new PredictionDtos.CompletedEvent(
                            e.getId().toString(), e.getTitle(),
                            categories.getOrDefault(e.getCategoryId(), "Other"),
                            s.capacity(), s.sold(), roundMoney(s.actualRevenue()),
                            (int) Math.round(100d * s.sold() / s.capacity()),
                            e.getEndDateTime() == null ? "PAST_END_UNKNOWN" : "COMPLETED"
                    );
                })
                .sorted(Comparator.comparingDouble(PredictionDtos.CompletedEvent::actualGrossRevenue).reversed())
                .toList();

        if (futureOrOngoing.isEmpty()) {
            return new PredictionDtos.DashboardResponse(
                    "NO_UPCOMING_EVENTS", "NONE", 0,
                    "No upcoming or ongoing published ticketed events. Historical results are below.",
                    null, List.of(), completedEvents);
        }

        // Train only on COMPLETED events from the requesting organization.
        completed.sort(Comparator.comparing(Event::getEndDateTime).reversed());
        List<Event> training = completed.size() > 300 ? completed.subList(0, 300) : completed;
        List<PredictionDtos.EventFeatures> historyFeatures = training.stream()
                .map(e -> toFeatures(e, stats, categories)).toList();
        List<PredictionDtos.EventFeatures> futureFeatures = futureOrOngoing.stream()
                .sorted(Comparator.comparing(Event::getStartDateTime))
                .map(e -> toFeatures(e, stats, categories)).toList();

        PredictionDtos.DashboardResponse forecast = predictionClient.forecast(
                new PredictionDtos.PredictionRequest(historyFeatures, futureFeatures));
        return new PredictionDtos.DashboardResponse(
                forecast.status(), forecast.method(), forecast.trainingEventCount(),
                forecast.message(), forecast.validationMaeTickets(),
                forecast.predictions() == null ? List.of() : forecast.predictions(), completedEvents);
    }

    private static double roundMoney(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private PredictionDtos.EventFeatures toFeatures(Event e, Map<UUID, Stats> stats,
                                                    Map<UUID, String> categories) {
        Stats s = stats.getOrDefault(e.getId(), new Stats(0, 0, 0, 0));
        int leadDays = e.getCreatedAt() == null ? 0 : (int) Math.max(0,
                Math.min(3650, Duration.between(e.getCreatedAt(), e.getStartDateTime()).toDays()));
        return new PredictionDtos.EventFeatures(
                e.getId().toString(), e.getTitle() == null ? "Untitled event" : e.getTitle(),
                categories.getOrDefault(e.getCategoryId(), "Other"),
                e.getCity() == null ? "Unknown" : e.getCity(),
                e.getStartDateTime().getDayOfWeek().getValue(),
                e.getStartDateTime().getMonthValue(), e.getStartDateTime().getHour(),
                s.capacity(), s.avgPrice(), s.sold(), leadDays, e.getStartDateTime().toString());
    }

    private Map<UUID, Stats> loadStats(List<Event> events) {
        if (events.isEmpty()) return Map.of();
        List<UUID> ids = events.stream().map(Event::getId).toList();
        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
        Map<UUID, Stats> result = new HashMap<>();

        // Weighted advertised ticket price, NOT actual paid revenue.
        jdbc.query("""
                SELECT event_id, COALESCE(SUM(total_capacity),0) AS capacity,
                       COALESCE(SUM(price * total_capacity) /
                                NULLIF(SUM(total_capacity),0),0) AS avg_price
                FROM ticket_tiers WHERE event_id IN (:ids) GROUP BY event_id
                """, params, rs -> {
            UUID id = rs.getObject("event_id", UUID.class);
            BigDecimal avg = rs.getBigDecimal("avg_price");
            result.put(id, new Stats(rs.getInt("capacity"), avg == null ? 0 : avg.doubleValue(), 0, 0));
        });

        // Count only issued tickets whose associated payment succeeded.
        jdbc.query("""
                SELECT o.event_id, COUNT(t.ticket_id) AS sold
                FROM orders o JOIN attendee_tickets t ON t.order_id = o.order_id
                WHERE o.event_id IN (:ids) AND o.payment_status = 'SUCCESS'
                GROUP BY o.event_id
                """, params, rs -> {
            UUID id = rs.getObject("event_id", UUID.class);
            Stats s = result.getOrDefault(id, new Stats(0, 0, 0, 0));
            result.put(id, new Stats(s.capacity(), s.avgPrice(),
                    Math.toIntExact(rs.getLong("sold")), s.actualRevenue()));
        });

        // Sum each successful order once (never join tickets here; that would multiply revenue).
        jdbc.query("""
                SELECT event_id, COALESCE(SUM(total_amount),0) AS revenue
                FROM orders WHERE event_id IN (:ids) AND payment_status = 'SUCCESS'
                GROUP BY event_id
                """, params, rs -> {
            UUID id = rs.getObject("event_id", UUID.class);
            Stats s = result.getOrDefault(id, new Stats(0, 0, 0, 0));
            BigDecimal revenue = rs.getBigDecimal("revenue");
            result.put(id, new Stats(s.capacity(), s.avgPrice(), s.sold(),
                    revenue == null ? 0 : revenue.doubleValue()));
        });
        return result;
    }
}
