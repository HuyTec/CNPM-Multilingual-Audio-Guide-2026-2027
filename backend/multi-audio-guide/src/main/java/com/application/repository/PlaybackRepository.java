package com.application.repository;

import com.application.dto.AnalyticsResponse.CountItem;
import com.application.model.GuideContent;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PlaybackRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private static final String PERIOD = """
            FROM analytics_events e WHERE e.event_type = 'PLAYBACK_STARTED'
            AND e.occurred_at >= :start AND e.occurred_at < :end
            """;

    public PlaybackRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean insert(UUID eventId, GuideContent guide) {
        return insert(eventId, guide, Instant.now(), false);
    }

    public boolean insert(UUID eventId, GuideContent guide, Instant occurredAt, boolean offline) {
        return jdbc.update("""
                INSERT INTO analytics_events(id, content_id, location_id, language_code, event_type, is_offline, occurred_at)
                VALUES (:id, :content, :location, :language, 'PLAYBACK_STARTED', :offline, :occurred)
                ON CONFLICT (id) DO NOTHING
                """, Map.of("id", eventId, "content", guide.contentId(), "location", guide.locationId(), "language", guide.languageCode(),
                        "offline", offline, "occurred", Timestamp.from(occurredAt))) == 1;
    }

    public Optional<UUID> contentForEvent(UUID eventId) {
        return jdbc.query("SELECT content_id FROM analytics_events WHERE id = :id",
                Map.of("id", eventId), (rs, row) -> rs.getObject(1, UUID.class)).stream().filter(java.util.Objects::nonNull).findFirst();
    }

    public void attribute(UUID eventId, UUID installationId, String platform) {
        jdbc.update("UPDATE analytics_events SET installation_id=:installation, client_platform=:platform WHERE id=:id",
                Map.of("id", eventId, "installation", installationId, "platform", platform));
    }

    public long count(Instant start, Instant end, boolean offline) {
        Long result = jdbc.queryForObject("SELECT count(*) " + PERIOD + " AND e.is_offline = :offline",
                Map.of("start", Timestamp.from(start), "end", Timestamp.from(end), "offline", offline), Long.class);
        return result == null ? 0 : result;
    }

    public List<CountItem> byLocation(Instant start, Instant end) {
        return jdbc.query("""
                SELECT COALESCE(l.name, 'Địa điểm không còn khả dụng') AS label, count(*) AS total
                FROM analytics_events e LEFT JOIN locations l ON l.id = e.location_id
                WHERE e.event_type = 'PLAYBACK_STARTED' AND e.occurred_at >= :start AND e.occurred_at < :end
                GROUP BY e.location_id, l.name ORDER BY total DESC, label, e.location_id
                """, period(start, end), (rs, row) -> new CountItem(rs.getString("label"), rs.getLong("total")));
    }

    public List<CountItem> byLanguage(Instant start, Instant end) {
        return jdbc.query("SELECT COALESCE(e.language_code, 'UNKNOWN') AS label, count(*) AS total " + PERIOD
                + " GROUP BY e.language_code ORDER BY total DESC, label", period(start, end),
                (rs, row) -> new CountItem(rs.getString("label"), rs.getLong("total")));
    }

    private Map<String, Object> period(Instant start, Instant end) {
        return Map.of("start", Timestamp.from(start), "end", Timestamp.from(end));
    }
}
