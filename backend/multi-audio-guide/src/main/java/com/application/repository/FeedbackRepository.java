package com.application.repository;

import com.application.dto.FeedbackDto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile("demo")
public class FeedbackRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private static final String SELECT = """
            SELECT f.*,COALESCE(l.name,'Không gắn địa điểm') AS location_name
            FROM feedbacks f LEFT JOIN locations l ON l.id=f.location_id
            """;
    private static final String FILTER = """
            WHERE f.created_at>=:start AND f.created_at<:end
            AND (:category='' OR f.category=:category) AND (:status='' OR f.status=:status)
            """;
    private static final RowMapper<Item> MAPPER = (rs, row) -> new Item(rs.getObject("id", UUID.class),
            rs.getObject("location_id", UUID.class), rs.getString("location_name"), rs.getString("category"),
            rs.getString("content") == null ? "" : rs.getString("content"), rs.getObject("rating", Integer.class), rs.getString("status"),
            rs.getTimestamp("created_at").toInstant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate().toString(),
            rs.getTimestamp("updated_at").toInstant(), rs.getTimestamp("created_at").toInstant());
    public FeedbackRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    private Map<String, ?> params(Criteria c) {
        return Map.of("start", Timestamp.from(c.start()), "end", Timestamp.from(c.end()), "category", c.category(),
                "status", c.status(), "size", c.size(), "offset", (long) (c.page() - 1) * c.size());
    }
    public long count(Criteria c) {
        return jdbc.queryForObject("SELECT count(*) FROM feedbacks f " + FILTER, params(c), Long.class);
    }
    public List<Item> list(Criteria c, boolean all) {
        return jdbc.query(SELECT + FILTER + " ORDER BY f.created_at DESC,f.id" + (all ? "" : " LIMIT :size OFFSET :offset"), params(c), MAPPER);
    }
    public List<String> categories() {
        return jdbc.queryForList("SELECT DISTINCT category FROM feedbacks ORDER BY category", Map.of(), String.class);
    }
    public Optional<Item> find(UUID id) {
        return jdbc.query(SELECT + " WHERE f.id=:id", Map.of("id", id), MAPPER).stream().findFirst();
    }
    public boolean advance(UUID id, String expected, String next) {
        return jdbc.update("UPDATE feedbacks SET status=:next,updated_at=CURRENT_TIMESTAMP WHERE id=:id AND status=:expected",
                Map.of("id", id, "expected", expected, "next", next)) == 1;
    }
}
