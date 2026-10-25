package com.application.repository;

import com.application.dto.ContentPackageDto.*;
import com.application.model.GuideContent;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@Profile("demo")
public class ContentPackageRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper json = JsonMapper.builder().build();
    private static final String FILTER = " AND (CASE WHEN :base THEN l.category_id=:category ELSE l.category_id<>:category END)";
    public ContentPackageRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    private Map<String, ?> parameters(boolean base) {
        return Map.of("base", base, "category", UUID.fromString("10000000-0000-0000-0000-000000000101"));
    }
    public List<Location> locations(boolean base) {
        return jdbc.query("""
                SELECT l.id,l.name,l.category_id,c.name AS category_name,
                       ST_Y(l.coordinates::geometry) AS lat,ST_X(l.coordinates::geometry) AS lng,l.geofence_radius_m,l.image_url,
                    COALESCE((SELECT jsonb_object_agg(lower(t.language_code),t.name) FROM location_translations t WHERE t.location_id=l.id),'{}'::jsonb)::text AS localized_names
                FROM locations l JOIN categories c ON c.id=l.category_id WHERE l.status='APPROVED' AND c.is_active
                """ + FILTER + " ORDER BY l.id", parameters(base), (rs, row) -> new Location(rs.getObject("id", UUID.class),
                rs.getString("name"), rs.getObject("category_id", UUID.class), rs.getString("category_name"),
                rs.getDouble("lat"), rs.getDouble("lng"), rs.getInt("geofence_radius_m"), localized(rs.getString("localized_names")),rs.getString("image_url")));
    }
    public List<GuideContent> guides(boolean base) {
        return jdbc.query("""
                SELECT DISTINCT ON (g.location_id,g.language_code,g.script_type)
                    g.id,g.location_id,COALESCE((SELECT t.name FROM location_translations t WHERE t.location_id=l.id AND t.language_code=g.language_code),l.name) AS name,g.language_code,g.script_type,g.script_text,a.storage_key
                FROM guide_content_revisions g JOIN locations l ON l.id=g.location_id
                JOIN categories c ON c.id=l.category_id JOIN languages lang ON lang.code=g.language_code
                LEFT JOIN audio_assets a ON a.content_id=g.id
                WHERE l.status='APPROVED' AND g.status='APPROVED' AND g.is_current AND c.is_active AND lang.is_active
                  AND g.script_text ~ '[^[:space:]]'
                """ + FILTER + " ORDER BY g.location_id,g.language_code,g.script_type,g.revision_no DESC,g.id", parameters(base),
                (rs, row) -> new GuideContent(rs.getObject("id", UUID.class), rs.getObject("location_id", UUID.class),
                        rs.getString("name"),rs.getString("language_code"),rs.getString("script_type"),rs.getString("script_text"),rs.getString("storage_key")));
    }
    /** One batch for all surveyed locations, rather than a query per package location. */
    public Geofencing geofencing(List<Location> locations) {
        var cooldown = jdbc.queryForList("SELECT cooldown_seconds FROM geofence_configuration WHERE singleton", Map.of(), Integer.class);
        if (cooldown.isEmpty() || locations.isEmpty()) return null;
        record Survey(String id, Instant at, Map<String, String> names) { }
        List<Survey> surveys = jdbc.query("""
                SELECT s.location_id,s.surveyed_at,(s.localized_names || COALESCE(
                    (SELECT jsonb_object_agg(t.language_code,t.name) FROM location_translations t WHERE t.location_id=s.location_id),
                    '{}'::jsonb))::text AS names
                FROM location_geofence_surveys s JOIN locations l ON l.id=s.location_id
                WHERE s.location_id IN (:ids) AND s.latitude=ST_Y(l.coordinates::geometry)
                  AND s.longitude=ST_X(l.coordinates::geometry) AND s.radius_m=l.geofence_radius_m
                  AND s.surveyed_at<=CURRENT_TIMESTAMP ORDER BY s.location_id
                """, Map.of("ids", locations.stream().map(Location::locationId).toList()), (rs, row) -> {
            Map<String, String> names = new LinkedHashMap<>();
            json.readTree(rs.getString("names")).properties().forEach(e -> names.put(e.getKey(), e.getValue().asString()));
            return new Survey(rs.getString(1), rs.getTimestamp(2).toInstant(), names);
        });
        Map<String, Map<String, String>> names = new LinkedHashMap<>();
        surveys.forEach(s -> names.put(s.id(), s.names()));
        return surveys.isEmpty() ? null : new Geofencing(cooldown.getFirst(), surveys.stream().map(Survey::at).max(Instant::compareTo).orElseThrow(), names);
    }
    public byte[] audio(String url) {
        return jdbc.queryForObject("SELECT data FROM admin_audio_uploads WHERE '/api/content-audio/'||id::text||'.'||format=:url", Map.of("url", url), byte[].class);
    }
    private static java.util.Map<String,String> localized(String raw) {
        var result=new java.util.LinkedHashMap<String,String>();
        tools.jackson.databind.json.JsonMapper.builder().build().readTree(raw).properties().forEach(e->result.put(e.getKey(),e.getValue().asString()));
        return result;
    }
}
