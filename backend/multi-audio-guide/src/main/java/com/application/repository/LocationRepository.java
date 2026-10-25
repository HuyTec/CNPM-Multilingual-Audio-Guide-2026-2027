package com.application.repository;

import com.application.model.Location;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class LocationRepository {
    private static final String SELECT_LOCATION = """
            SELECT l.id, l.name, l.category_id, c.name AS category_name,
                   ST_Y(l.coordinates::geometry) AS latitude,
                   ST_X(l.coordinates::geometry) AS longitude, l.geofence_radius_m,
                    COALESCE((SELECT jsonb_object_agg(lower(t.language_code),t.name) FROM location_translations t WHERE t.location_id=l.id),'{}'::jsonb)::text AS localized_names
            """;
    private static final String VISIBLE_LOCATIONS = """
            FROM locations l JOIN categories c ON c.id = l.category_id
            WHERE l.status = 'APPROVED' AND c.is_active = TRUE
            """;
    private static final RowMapper<Location> ROW_MAPPER = (rs, rowNum) -> new Location(
            rs.getObject("id", UUID.class), rs.getString("name"),
            rs.getObject("category_id", UUID.class), rs.getString("category_name"),
            rs.getObject("latitude", Double.class), rs.getObject("longitude", Double.class),
            rs.getObject("geofence_radius_m", Integer.class), localized(rs.getString("localized_names")));

    private final NamedParameterJdbcTemplate jdbc;

    public LocationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Rank exact/prefix/substring matches; bind user input and use UUID to break ordering ties. */
    public List<Location> search(String keyword, UUID categoryId, int page, int size) {
        String order = keyword.isEmpty() ? "" : """
                CASE WHEN lower(l.name) = lower(:keyword) THEN 0
                     WHEN l.name ILIKE :prefix ESCAPE '!' THEN 1 ELSE 2 END,
                """;
        String sql = SELECT_LOCATION + filters(keyword, categoryId)
                + " ORDER BY " + order + " lower(l.name), l.id LIMIT :limit OFFSET :offset";
        return jdbc.query(sql, parameters(keyword, categoryId)
                .addValue("limit", size).addValue("offset", (long) page * size), ROW_MAPPER);
    }

    public long count(String keyword, UUID categoryId) {
        Long total = jdbc.queryForObject("SELECT count(*) " + filters(keyword, categoryId),
                parameters(keyword, categoryId), Long.class);
        return total == null ? 0 : total;
    }

    public Optional<Location> findPublicById(UUID locationId) {
        return jdbc.query(SELECT_LOCATION + VISIBLE_LOCATIONS + " AND l.id = :id",
                Map.of("id", locationId), ROW_MAPPER).stream().findFirst();
    }

    private String filters(String keyword, UUID categoryId) {
        return VISIBLE_LOCATIONS
                + (keyword.isEmpty() ? "" : " AND l.name ILIKE :pattern ESCAPE '!'")
                + (categoryId == null ? "" : " AND l.category_id = :categoryId");
    }

    /** Escape LIKE wildcards so a user's %, _ and ! are searched literally. */
    private MapSqlParameterSource parameters(String keyword, UUID categoryId) {
        String literal = keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return new MapSqlParameterSource().addValue("keyword", keyword)
                .addValue("pattern", "%" + literal + "%").addValue("prefix", literal + "%")
                .addValue("categoryId", categoryId);
    }
    private static java.util.Map<String,String> localized(String raw) {
        var result=new java.util.LinkedHashMap<String,String>();
        tools.jackson.databind.json.JsonMapper.builder().build().readTree(raw).properties().forEach(e->result.put(e.getKey(),e.getValue().asString()));
        return result;
    }
}
