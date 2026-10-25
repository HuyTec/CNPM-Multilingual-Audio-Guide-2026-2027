package com.application.repository;

import com.application.dto.VisitorHeartbeat;
import com.application.dto.VisitorStats.PlatformStats;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public class VisitorRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public VisitorRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void touch(VisitorHeartbeat request) {
        jdbc.update("""
            INSERT INTO visitor_installations(installation_id, platform) VALUES (:id, :platform)
            ON CONFLICT (installation_id, platform) DO UPDATE SET last_seen_at = CURRENT_TIMESTAMP
            """, Map.of("id", request.installationId(), "platform", request.platform().name()));
    }

    public List<PlatformStats> stats(Instant activeSince) {
        return jdbc.query("""
            SELECT p.platform, count(v.installation_id) AS total,
                   count(v.installation_id) FILTER (WHERE v.last_seen_at >= :since) AS active
            FROM (VALUES ('WEB'), ('ANDROID')) AS p(platform)
            LEFT JOIN visitor_installations v ON v.platform = p.platform
            GROUP BY p.platform ORDER BY p.platform
            """, Map.of("since", Timestamp.from(activeSince)),
            (rs, row) -> new PlatformStats(rs.getString("platform"), rs.getLong("total"), rs.getLong("active")));
    }
}
